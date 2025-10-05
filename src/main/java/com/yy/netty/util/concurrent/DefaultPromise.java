package com.yy.netty.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.yy.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @param <V>
 * @Description:promise的默认实现类,是一个很重要的线程协作工具
 * <p>
 * 在netty中，这个可以看作一个不完整的futuretask，他们二者的区别在于:
 * futuretask可以作为一个任务被线程或线程池执行，不能手动设置结果,futuretask对任务的执行过程和结果都要负责
 * 而该类则不能当做任务被线程或线程池执行，但可以手动把外部线程得到的结果赋值给result属性，该类只对结果负责，通过结果协调线程
 * </p>
 * <p>
 * 任务状态流转总结：
 * 1、result为null，说明任务刚开始或者在执行中；
 * 2、result为SUCCESS，说明任务已经成功完成,对应任务返回为null的情况；其他任务返回不是null的成功情况，result对应的就是V类型的真实结果
 * 3、result为UNCANCELLABLE，说明当前任务的状态为不能被取消，表达的只是一种状态，不是结果，任务还未结束；
 * 4、result为CauseHolder，说明任务执行中出现了异常，异常有两种可能性：（1）主动提前取消，对应CancellableException；（2）其他过程异常
 * 以上result一旦被设置为SUCCESS，V，CauseHolder，代表任务结束了；result为其他值时，代表的是一种状态，任务还在继续；
 * </p>
 */
public class DefaultPromise<V> extends AbstractFuture<V> implements Promise<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);

    // 任务成功完成时对应的结果（返回值为void的情况）
    private static final Object SUCCESS = new Object();
    // 任务的一种状态，表达当前任务是不能被取消的
    private static final Object UNCANCELLABLE = new Object();

    //原子更新器，用来并发时更新result的值
    private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> RESULT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");

    // 该结果对应着任务执行的结果，result一旦被设置结果意味着任务结束了（除了被设置为UNCANCELLABLE）
    private volatile Object result;

    // 需要通知的监听器,通常对应着一个默认实现DefaultFutureListeners
    private Object listeners;

    // 一定会出现这样的情况，当外部线程调用该类的get方法时，如果任务还未执行完毕，则外部线程将视情况阻塞。每当一个外部线程阻塞时，该属性便加一，线程继续执行后，该属性减一
    private short waiters;

    // 标志位，防止并发通知的情况出现，如果为ture，则说明有线程通知监听器了，为false则说明没有。
    private boolean notifyingListeners;

    // 每一个promise都要有执行器来执行异步任务，比如监听器的回调
    private final EventExecutor executor;


    public DefaultPromise(EventExecutor executor) {
        this.executor = checkNotNull(executor, "executor");
    }

    /**
     * 得到执行器
     *
     * @return
     */
    protected EventExecutor executor() {
        return executor;
    }


    /**
     * 主动设置任务成功结束的结果
     *
     * @param result
     * @return
     */
    @Override
    public Promise<V> setSuccess(V result) {
        if (setSuccess0(result)) {
            return this;
        }
        // 设置失败，抛出异常
        throw new IllegalStateException("complete already: " + this);
    }

    private boolean setSuccess0(V result) {
        // 设置成功结果，如果结果为null，则将SUCCESS赋值给result
        return setValue0(result == null ? SUCCESS : result);
    }

    /**
     * 给成员变量result设置值，符合如下两种情况，result可以被赋值：
     * 1、result为null，可以设置任务结果
     * 2、result为UNCANCELLABLE，任务不能被取消，可以设置任务结果
     * result一旦被设置结果意味着任务结束了,如果注册有监听器，则进行回调
     *
     * @param objectResult
     * @return
     */
    private boolean setValue0(Object objectResult) {
        //result还未被赋值时，原子更新器可以将结果赋值给result
        if (RESULT_UPDATER.compareAndSet(this, null, objectResult) || RESULT_UPDATER.compareAndSet(this, UNCANCELLABLE, objectResult)) {
            // 唤醒阻塞的线程，并检测是否有监听器
            if (checkNotifyWaiters()) {
                // 注册了监听器，进行回调
                notifyListeners();
            }
            return true;
        }
        // result被赋值过，则说明任务已经结束，不能再设置结果了
        return false;
    }

    /**
     * 监听器回调
     */
    private void notifyListeners() {
        //得到执行器
        EventExecutor executor = executor();
        //如果正在执行方法的线程就是执行器的线程，就立刻通知监听器执行方法
        if (executor.inEventLoop(Thread.currentThread())) {
            notifyListenersNow();
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    notifyListenersNow();
                }
            });
        }
    }

    private static void safeExecute(EventExecutor executor, Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (Throwable t) {
//            logger.warn("Failed to submit a listener notification task.", t);
            throw new RuntimeException("Failed to submit a listener notification task. Event loop shut down?", t);
        }
    }

    /**
     * 完成对注册在当前DefaultPromise对象上的监听器的回调
     */
    private void notifyListenersNow() {
        Object listeners;
        // 并发控制，防止多次回调
        synchronized (this) {
            //notifyingListeners这个属性如果为ture，说明已经有线程通知监听器了。或者当监听器属性为null，这时候直接返回即可。
            if (notifyingListeners || this.listeners == null) {
                return;
            }
            //如果没有通知，把notifyingListeners设置为ture
            notifyingListeners = true;
            listeners = this.listeners;
            //将listeners属性设置为null，代表通知过了已经，这时候锁就要被释放了，当有其他线程进入该代码块时，就会进入if判断直接退出了
            this.listeners = null;
        }
        // 走到这里，说明当前线程拿到了待通知的监听器，那么就需要去遍历监听器，并执行监听器的回调，
        // 这里面有一个特别极端的情况，就是在监听器回调的过程中，有新的监听器注册，那么就会导致listeners属性被赋值，这时候就要求再来一次循环，再次完成对监听器的回调
        for (; ; ) {
            //如果listerers是DefaultFutureListeners该类型，则说明有多个监听器，是个监听器数组，要执行 通知多个数组的方法
            if (listeners instanceof DefaultFutureListeners) {
                notifyListeners0((DefaultFutureListeners) listeners);
            } else {
                //说明只有一个监听器。
                notifyListeners0(this, (GenericFutureListener<?>)listeners);
            }
            //通知完成后继续上锁
            synchronized (this) {
                //这里再次加锁是因为方法结束之后notifyingListeners的值要重置。
                if (this.listeners == null) {
                    notifyingListeners = false;
                    //重置之后退出即可
                    return;
                }
                // 走到这里，说明listeners属性被赋值了，这时候需要再次对监听器进行回调
                listeners = this.listeners;
                this.listeners = null;
            }
        }
    }

    private void notifyListeners0(DefaultFutureListeners listeners) {
        //得到监听器数组
        GenericFutureListener<? extends Future<?>>[] a = listeners.listeners();
        //遍历数组，一次通知监听器执行方法
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            notifyListeners0(this, a[i]);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void notifyListeners0(Future future, GenericFutureListener listener) {
        try {
            listener.operationComplete(future);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * 唤醒阻塞的线程，并检测是否有监听器
     *
     * @return
     */
    private synchronized boolean checkNotifyWaiters() {
        if (waiters > 0) {
            // 唤醒所有阻塞在当前DefaultPromise对象上的线程
            notifyAll();
        }
        return listeners != null;
    }

    /**
     * 尝试设置任务成功结束的结果
     *
     * @param result
     * @return
     */
    @Override
    public boolean trySuccess(V result) {
        return setSuccess0(result);
    }

    /**
     * 设置任务失败结束的结果
     *
     * @param cause
     * @return
     */
    @Override
    public Promise<V> setFailure(Throwable cause) {
        if (setFailure0(cause)) {
            return this;
        }
        throw new IllegalStateException("complete already: " + this,  cause);
    }

    private boolean setFailure0(Throwable cause) {
        //设置失败结果，也就是包装过的异常信息
        return setValue0(new CauseHolder(checkNotNull(cause, "cause")));
    }


    @Override
    public boolean tryFailure(Throwable cause) {
        return setFailure0(cause);
    }

    /**
     * 设置任务为不可取消
     *
     * @return
     */
    @Override
    public boolean setUncancellable() {
        //用原子更新器更新result的值，这时候result还未被赋值，只有result为null时，才可以设置为不可取消，其他状态说明任务已经结束了，或者已经被设置为不可取消了
        if (RESULT_UPDATER.compareAndSet(this, null, UNCANCELLABLE)) {
            return true;
        }
        //走到这里说明设置失败了，意味着任务不可取消，这就对应两种结果，一是任务已经执行成功了，无法取消
        //二就是任务已经被别的线程设置为不可取消了。
        Object res = this.result;
        // 如果已经被设置为不可取消了，那么就返回true，如果是任务已经执行结束了，那么就返回false
        // 这里面任务结束有两种情况：1、任务成功结束；2、任务异常结束（这里面也分两种，一种就是过程异常，一种时主动取消异常）
        return res == UNCANCELLABLE;
    }

    /**
     * 判断任务是否结束,对应两种情况：
     * 1、成功,对应result为V值或者SUCCESS
     * 2、失败,对应result为CauseHolder
     *
     * @param result
     * @return
     */
    private static boolean isDone0(Object result) {
        return result != null && result != UNCANCELLABLE;
    }

    /**
     * 判断任务是否被取消,对应result为CauseHolder，且cause为CancellationException
     *
     * @param result
     * @return
     */
    private static boolean isCancelled0(Object result) {
        return result instanceof CauseHolder && ((CauseHolder) result).cause instanceof CancellationException;
    }


    @Override
    public Promise<V> await() throws InterruptedException {
        return null;
    }

    @Override
    public Promise<V> awaitUninterruptibly() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return false;
    }

    /**
     * 获取当前任务结果,如果result不是成功状态的具体结果，那么都返回null，表示任务当前就是没有结果的
     *
     * @return
     */
    @Override
    public V getNow() {
        Object result = this.result;
        if (result instanceof CauseHolder || result == SUCCESS || result == UNCANCELLABLE) {
            return null;
        }
        return (V) result;
    }

    /**
     * 取消当前任务
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        //原子更新器得到当前resuli的值，如果为null，说明任务还未执行完成，并且没有被取消
        if (RESULT_UPDATER.compareAndSet(this, null, new CauseHolder(new CancellationException()))) {
            // 任务取消，代表任务结束了，那么唤醒阻塞的线程
            if (checkNotifyWaiters()) {
                //通知所有监听器执行
                notifyListeners();
            }
            return true;
        }
        // 走到这里说明任务已经结束，或者被取消了,或者任务为不可取消状态
        return false;
    }

    /**
     * 判断任务是否被取消了
     *
     * @return
     */
    @Override
    public boolean isCancelled() {
        return isCancelled0(this.result);
    }

    /**
     * 判断任务是否结束了
     * @return
     */
    @Override
    public boolean isDone() {
        return isDone0(this.result);
    }

    /**
     * 判断任务是否成功结束,对应result为V值或者SUCCESS
     *
     * @return
     */
    @Override
    public boolean isSuccess() {
        Object result = this.result;
        //result不为空，并且不等于被取消，并且不属于被包装过的异常类
        return result != null && result != UNCANCELLABLE && !(result instanceof CauseHolder);
    }

    /**
     * 判断任务是否可取消,只有result为null时，任务可取消
     *
     * @return
     */
    @Override
    public boolean isCancellable() {
        return this.result == null;
    }

    /**
     * 获取任务执行时的异常
     *
     * @return
     */
    @Override
    public Throwable cause() {
        Object result = this.result;
        //如果得到的结果属于包装过的异常类，说明任务执行时是有异常的，直接从包装过的类中得到异常属性即可，如果不属于包装过的异常类，则直接
        //返回null即可
        return (result instanceof CauseHolder) ? ((CauseHolder) result).cause : null;
    }

    @Override
    public Promise<V> sync() throws InterruptedException {
        return null;
    }

    @Override
    public Promise<V> syncUninterruptibly() {
        return null;
    }

    @Override
    public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        return null;
    }

    @Override
    public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        return null;
    }

    @Override
    public Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        return null;
    }

    @Override
    public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        return null;
    }

    /**
     * @Description:一个私有的静态内部类，对异常的包装
     */
    private static final class CauseHolder {

        final Throwable cause;

        CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }


}
