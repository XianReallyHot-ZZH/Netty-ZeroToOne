package com.yy.netty.test.future;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;


/**
 * 未来任务，是一个任务，同时也是一个线程协调工具
 *
 * @param <V>
 */
public class FutureTask<V> implements RunnableFuture<V> {

    // 使用unsafe类，获取对象属性的偏移量，后续用于进行并发控制操作
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;

    //初始状态，外部线程进行get的时候会进行等待
    private static final int NEW = 0;
    //正在赋值，还没有彻底完成，外部线程进行get的时候会进行等待;最终该状态会演化成NORMAL状态或者EXCEPTIONAL状态
    private static final int COMPLETING = 1;
    //已经正常完成了，外部线程进行get的时候会得到一个正常运行后的结果
    private static final int NORMAL = 2;
    //执行过程中出现异常，那么外部线程进行get的时候就会抛出ExecutionException
    private static final int EXCEPTIONAL = 3;
    //取消该任务，那么外部线程进行get的时候就会抛出CancellationException
    private static final int CANCELLED = 4;
    //中断线程，但是 不是直接中断线程，而是设置一个中断变量，线程还未中断；那么外部线程进行get的时候就会抛出CancellationException
    private static final int INTERRUPTING = 5;
    //任务已经被打断了，那么外部线程进行get的时候就会抛出CancellationException
    private static final int INTERRUPTED = 6;

    static {
        try {
            UNSAFE = getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }

    }

    //状态值，表示的就是该FutureTask执行到哪一步了
    private volatile int state;

    // 用户传进来的要被执行的又返回值的任务
    private Callable<V> callable;

    // 任务的最终结果,这里不能设置为V类型是因为返回值有可能是个异常，异常对象也是会利用这个变量进行存储的
    private Object outcome;

    // 任务执行的线程
    private volatile Thread runner;

    // 是一个包装线程的对象。并且是链表的头节点
    private volatile WaitNode waiters;

    /**
     * 获取Unsafe对象,普通类调用Unsafe类本身提供的getUnsafe方法是不行的，会报错，拿不到的，所以我们这里直接重写，使用反射强制获取
     *
     * @return
     */
    private static sun.misc.Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (sun.misc.Unsafe) theUnsafe.get(null);   // 获取Unsafe对象,Unsafe对象是一个静态成员变量，所以这里传入的操作对象是null
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
    }


    @Override
    public void run() {
        // 1.如果当前任务状态不是NEW，说明任务已经已经执行过了，或者被取消了，那么就不用再执行了
        // 2.如果当前任务已经被设置了执行线程，那么说明当前任务至少已经被分配了，在这里进行并发控制，防止多次执行
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) {
            return;
        }
        // 到这说明当前任务是NEW状态，接下来就要执行了
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                // 无论如何，任务要么运行成功，要么运行过程中出现了异常，总之都会设置结果到outcome中
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    //把出现的异常赋值给成员变量
                    setException(ex);
                }
                if (ran) {
                    //走到这就意味着任务正常结束，可以正常返把执行结果赋值给成员变量
                    set(result);
                }
            }
        } finally {
            // 和执行相关的资源释放
            runner = null;
            // 有可能在任务执行期间，任务被外部线程调用cancel进行显式中断，那么这里就要针对这种情况进行处理
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
    }

    /**
     * 任务运行，出现异常，最终的值设置逻辑
     * @param t
     */
    protected void setException(Throwable t) {
        // 1.如果当前任务状态不是NEW，说明任务已经已经执行过了，或者被取消了，任务的结果就已经确定了，那么执行的结果其实就该被丢弃
        // 2.并发处理
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL);
            // 唤醒get上的阻塞线程
            finishCompletion();
        }
    }

    /**
     * 任务运行，正常借宿，最终的值设置逻辑
     *
     * @param v
     */
    protected void set(V v) {
        // 1.如果当前任务状态不是NEW，说明任务已经已经执行过了，或者被取消了，任务的结果就已经确定了，那么执行的结果其实就该被丢弃
        // 2.并发处理
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL);
            // 唤醒get上的阻塞线程
            finishCompletion();
        }
    }

    /**
     * 把等待队列中的有效节点的线程唤醒，也就是那些调用了get方法，然后阻塞在该方法处的外部线程
     */
    private void finishCompletion() {
        for (WaitNode q; (q = waiters) != null;) {
            // 处理并发，有可能会存在多个线程同时调用该方法，而唤醒链表中的线程只需要一遍就行了
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        // 进行唤醒
                        LockSupport.unpark(t);
                    }
                    // 链表往下走
                    WaitNode next = q.next;
                    if (next == null) {
                        break;
                    }
                    q.next = null;
                    q = next;
                }
                break;
            }
        }
    }


    /**
     * 执行任务的线程对外部线程可能的中断请求的处理
     * @param s
     */
    private void handlePossibleCancellationInterrupt(int s) {
        if (s == INTERRUPTING) {
            while (state == INTERRUPTING) {     // 这里可以写循环，是因为INTERRUPTING是一定会转变成INTERRUPTED状态的，体现在cancel的实现中，INTERRUPTING到INTERRUPTED状态的转换有一定间隔，所以这里用循环过度
                //让出cpu让其他线程来执行任务
                Thread.yield();
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // FutureTask只能在任务没结束之前取消。并且要有并发控制，不支持多次取消,只有一次机会
        if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) {
            return false;
        }
        try {
            if (mayInterruptIfRunning) {
                try {
                    // 如果允许中断，那么就要尝试去中断执行中的线程
                    Thread t = runner;
                    if (t != null) {
                        // 外部线程去中断执行中的线程，只能这么做，至于执行中的线程是否被中断成功，那只能去看那个执行中的任务支不支持响应中断了，这里是做不了其他的事情的
                        t.interrupt();
                    }
                } finally {
                    // 在这里保证INTERRUPTING是一定能转化成INTERRUPTED状态的
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }

            }
        } finally {
            // 一旦取消，那么说明其实等待线程对任务的结果已经不关心了，等待线程只想直接退出，所以这里就可以把等待线程直接都唤醒了
            finishCompletion();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        // CANCELLED、INTERRUPTING、INTERRUPTED都算是被取消了
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        // 状态不为NEW，说明任务已经执行结束了，或者被取消了，那就都算是Done了
        return state != NEW;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 说明当前的任务还没真正结束，有可能还没开始，或者正在执行中，或者正在赋值中，那么为了拿到最终的有效结果，是需要进行等待的
        if (s <= COMPLETING) {
            //开始无限等待，这个等待，指的是外部调用get方法的线程等待，这个等到的结果有可能是NORMAL，也可能是EXCEPTIONAL，或者三种取消的状态CANCELLED、INTERRUPTED、INTERRUPTING
            s = awaitDone(false, 0L);
        }
        // 走到这，说明要么是提前被唤醒，要么是没有等待，任务就已经被取消了，那无论如何，都要给等待的线程返回符合各个状态语义的结果
        return report(s);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        int s = state;
        // 说明当前的任务还没真正结束，有可能还没开始，或者正在执行中，或者正在赋值中，那么为了拿到最终的有效结果，是需要进行等待的，
        // 开始有限时的等待，这个等待，指的是外部调用get方法的线程等待，这个等到的结果有可能是NEW、COMPLETING、NORMAL，也可能是EXCEPTIONAL，或者三种取消的状态CANCELLED、INTERRUPTED、INTERRUPTING
        if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
            // NEW、COMPLETING状态说明任务还没开始，或者正在执行中，或者正在赋值中，总之是还没结束，那么就抛出一个超时异常
            throw new TimeoutException();
        }
        // 走到这，说明要么是提前被唤醒，要么是没有等待，任务就已经被取消了，那无论如何，都要给等待的线程返回符合各个状态语义的结果
        return report(s);
    }

    /**
     * 返回符合各个状态语义的结果, 调用该方法时，只可能有五种状态结果：NORMAL、EXCEPTIONAL、CANCELLED、INTERRUPTED、INTERRUPTING，其他两种状态，NEW和COMPLETING,会在其他地方被处理成超时异常
     *
     * @param s
     * @return
     * @throws ExecutionException
     */
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        //如果现在的这个s值等于NORMAL，就意味着我们的任务已经正常完成了，就可以直接返回执行结果
        if (s == NORMAL) {
            return (V) x;
        }
        //如果是已经被取消的状态，就会直接抛出一个异常
        if (s >= CANCELLED) {
            throw new CancellationException();
        }
        throw new ExecutionException((Throwable) x);
    }

    /**
     * 等待任务结束，返回任务状态
     *
     * @param timed     第一个参数，是允许限时阻塞，如果是false，就一直阻塞，等待执行结果返回才继续运行
     * @param nanos     等待时长
     * @return
     */
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        // 这里死循环是为了解决线程被虚假唤醒的问题，同时也让内部逻辑能够在一次次进入循环后根据当时的情况进行逻辑流转
        for (;;) {
            //判断当前线程是否被中断，中断了就将该线程从等待队列中移除，这里的线程指的是外部调用get方法的线程,基本上只有一次机会进入该逻辑
            if (Thread.interrupted()) {
                removeWaiter(q);
                // 当前等待线程响应中断，抛出中断异常
                throw new InterruptedException();
            }

            int s = state;
            //这时候说明任务已经完成了
            if (s > COMPLETING) {
                if (q != null) {
                    // 设置为无效节点，如果当前节点已经被放置进链表，未来只要有地方调用了removeWaiter方法该无效节点都会被移除
                    q.thread = null;
                }
                return s;
            } else if (s == COMPLETING) {
                // 任务正在被赋值，要马上结束了，等待线程让出时间片，待下次进入循环后，进入"s > COMPLETING"逻辑分值
                Thread.yield();
            } else if (q == null) {
                //一般第一次循环，会进入到这里，在这里q被创建了，把外部调用get方法的线程封装到WaitNode节点中。
                //该节点会被添加到一个队列中，实际上，所有在此阻塞的外部线程都会被包装成WaitNode节点，
                q = new WaitNode();
            } else if (!queued) {
                //一般第二次循环，会进入到这里，这里就是在队列头部搞了一个头节点，头节点就是最晚进来的那个线程，当然，线程都被WaitNode包装着
                //头节点实际上就是WaitNode对象。越晚进来的线程会排在链表的头部，谁最晚进来，谁就是头节点
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {
                nanos = deadline - System.nanoTime();
                // 超时了
                if (nanos <= 0L) {
                    // 只移除自己，不要移除别人
                    removeWaiter(q);
                    return state;
                }
                // 进行限时阻塞
                LockSupport.parkNanos(this, nanos);
            } else {
                //到这里是一直阻塞，可以看到阻塞采用的是 LockSupport.park方式。
                LockSupport.park(this);
            }


        }
    }

    /**
     * 移除一个等待的线程,实现上要考虑并发问题
     *
     * @param node
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            //把该节点的线程变量设置为null，为什么弄成null呢？因为这是在链表中找到要删除的节点的判断依据
            //如果该节点的thread为null，就说明该节点是一个无效节点，就应该从链表中移除
            node.thread = null;
            retry:
            for (;;) {
                //pred前驱节点，q就是当前节点，s就是后节点,这里其实就是链表往后流转，直到链表尾部
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    //把当前节点的下一个节点，赋值给s
                    s = q.next;
                    if (q.thread != null) {
                        // 当前节点不是无效节点，就跳过，继续往下找
                        pred = q;
                    } else if (pred != null) {
                        // 当前节点是无效节点，并且是有前驱节点的，就从链表中移除
                        pred.next = s;
                        // 并发场景下，有可能前驱节点也编程无效节点了，那么就需要重新开始循环，从链表头开始再来一遍
                        if (pred.thread == null) {
                            continue retry;
                        }
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) {
                        // 当前节点是无效节点，并且没有前驱节点，那么直接从链表头尝试删除，这里处理了并发的情况
                        continue retry;
                    }
                }
                break;
            }
        }
    }


    /**
     * 等待节点，所有的节点会被串成链表这种数据结构
     */
    static final class WaitNode {
        //外部调用get方法的线程会赋值给该成员变量
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() {
            thread = Thread.currentThread();
        }
    }

}
