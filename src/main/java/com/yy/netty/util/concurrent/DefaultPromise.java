package com.yy.netty.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.yy.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @Description:promise的默认实现类,是一个很重要的线程协作工具
 * <p>
 *     在netty中，这个可以看作一个不完整的futuretask，他们二者的区别在于:
 *     futuretask可以作为一个任务被线程或线程池执行，不能手动设置结果,futuretask对任务的执行过程和结果都要负责
 *     而该类则不能当做任务被线程或线程池执行，但可以手动把外部线程得到的结果赋值给result属性，该类只对结果负责，通过结果协调线程
 * </p>
 * <p>
 *     任务状态流转总结：
 *     1、result为null，说明任务刚开始或者在执行中；
 *     2、result为SUCCESS，说明任务已经成功完成,对应任务返回为null的情况；其他任务返回不是null的成功情况，result对应的就是V类型的真实结果
 *     3、result为UNCANCELLABLE，说明当前任务的状态为不能被取消，表达的只是一种状态，不是结果，任务还未结束；
 *     4、result为CauseHolder，说明任务执行中出现了异常，异常有两种可能性：（1）主动提前取消，对应CancellableException；（2）其他过程异常
 *     以上result一旦被设置为SUCCESS，V，CauseHolder，代表任务结束了；result为其他值时，代表的是一种状态，任务还在继续；
 * </p>
 *
 * @param <V>
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
     * result一旦被设置结果意味着任务结束了
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
            logger.warn("Failed to submit a listener notification task.", t);
//            throw new RuntimeException("Failed to submit a listener notification task. Event loop shut down?", t);
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
        for (;;) {

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
        return false;
    }

    @Override
    public Promise<V> setFailure(Throwable cause) {
        return null;
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        return false;
    }

    @Override
    public boolean setUncancellable() {
        return false;
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

    @Override
    public V getNow() {
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
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
}
