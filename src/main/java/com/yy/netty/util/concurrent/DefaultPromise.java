package com.yy.netty.util.concurrent;

import com.yy.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.yy.netty.util.internal.ObjectUtil.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

    /**
     * 在任务还未结束的时候，调用该方法的线程会被阻塞,支持当前线程抛出中断异常
     * 该方法在AbstractFuture抽象类中被调用了，当执行还没有结果的时候，外部线程调用get方法时，会进一步调用该方法进行阻塞。
     *
     * @return
     * @throws InterruptedException
     */
    @Override
    public Promise<V> await() throws InterruptedException {
        //如果已经执行完成，直接返回即可
        if (isDone()) {
            return this;
        }

        // 如果当前线程中断，支持直接抛出异常
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        //检查是否死锁，如果是死锁直接抛出异常，在这里可以进一步思考一下，哪些情况会发生死锁
        //如果熟悉了netty之后，就会发现，凡事结果要赋值到promise的任务都是由netty中的单线程执行器来执行的
        //执行每个任务的执行器是和channel绑定的。如果某个执行器正在执行任务，但是还未获得结果，这时候该执行器
        //又来获取结果，一个线程怎么能同时执行任务又要唤醒自己呢，所以必然会产生死锁
        checkDeadLock();
        //wait要和synchronized一起使用，在futurtask的源码中，这里使用了LockSupport.park方法。
        synchronized (this) {
            // 再一次判断任务是否执行完成，如果完成直接返回，不成功进入循环，这里循环是为了解决线程虚假唤醒问题
            while (!isDone()) {
                //waiters字段加一，记录在此阻塞的线程数量
                incWaiters();
                try {
                    //释放锁并等待，直到任务结束，由其他线程来唤醒这里
                    wait();
                    // 这里没有捕获线程中断异常，为的就是支持中断异常的抛出
                } finally {
                    //等待结束waiters字段减一
                    decWaiters();
                }
            }
        }

        return this;
    }

    private void decWaiters() {
        --waiters;
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters: " + this);
        }
        ++waiters;
    }

    private void checkDeadLock() {
        //得到执行器
        EventExecutor executor = executor();
        //判断是否为死锁，之前已经解释过这个问题了，其实就是等待线程和任务执行线程不能是同一个，任务执行都是在EventExecutor中的，所以其实就是判断当前线程和任务执行线程是不是同一个
        if (executor != null && executor.inEventLoop(Thread.currentThread())) {
            throw new BlockingOperationException(toString());
        }
    }

    /**
     * 在任务还未结束的时候，调用该方法的线程会被阻塞,不能抛出中断异常
     *
     * @return
     */
    @Override
    public Promise<V> awaitUninterruptibly() {
        //如果已经执行完成，直接返回即可
        if (isDone()) {
            return this;
        }

        checkDeadLock();

        // 当前线程是否已经抛出中断异常，如果抛出了，那么标记一下，
        // 因为这个方法是不支持抛出中断异常，会在内部吞掉异常的，这个标志最后用于恢复线程的中断标志，交给调用方处理
        boolean interrupted = false;
        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    wait();
                } catch (InterruptedException e) {
                    // 在这里进行主动捕获中断异常，不让其抛出
                    interrupted = true;
                } finally {
                    decWaiters();
                }
            }
        }
        //如果发生异常，则给调用该方法的线程设置中断标志
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return this;
    }

    /**
     * 支持超时时间的await
     *
     * @param timeout
     * @param unit
     * @return  true:任务结束；false:任务未结束
     * @throws InterruptedException
     */
    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(MILLISECONDS.toNanos(timeoutMillis), true);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toNanos(timeout), false);
        } catch (InterruptedException e) {
            //不会抛出异常
            throw new InternalError();
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(MILLISECONDS.toNanos(timeoutMillis), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /**
     * 真正让线程阻塞等待的方法
     * 返回结果，true:任务结束；false:任务未结束
     *
     * @param timeoutNanos
     * @param interruptable
     * @return
     */
    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        // 任务结束则直接返回
        if (isDone()) {
            return true;
        }

        //传入的时间小于0,则无需等待，直接返回任务是否结束
        if (timeoutNanos <= 0) {
            return isDone();
        }

        //interruptable为true则允许抛出中断异常，为false则不允许，判断当前线程是否被中断了
        //如果都为true则抛出中断异常
        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException();
        }

        //检查死锁
        checkDeadLock();

        //获取当前纳秒时间
        long startTime = System.nanoTime();
        //用户设置的等待时间
        long waitTime = timeoutNanos;
        //是否出现中断异常的标记
        boolean interrupted = false;

        try {
            // 避免虚假唤醒
            for (;;) {
                synchronized (this) {
                    //再次判断是否执行完成，如果结束，可直接返回
                    if (isDone()) {
                        return true;
                    }
                    //如果没有执行完成，则开始阻塞等待，阻塞线程数加一
                    incWaiters();
                    // 进行阻塞
                    try {
                        wait(waitTime / 1_000_000, (int) (waitTime % 1_000_000));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            // 如果是可中断的，那么直接抛出
                            throw e;
                        } else {
                            // 如果不可中断的，那么记录下中断状态，等待结束的时候恢复中断状态
                            interrupted = true;
                        }
                    } finally {
                        //阻塞线程数减一
                        decWaiters();
                    }
                }
                //走到这里说明线程被唤醒了
                if (isDone()) {
                    return true;
                } else {
                    //可能是虚假唤醒。
                    //得到新的等待时间，如果等待时间小于0，表示已经阻塞了用户设定的等待时间。如果waitTime大于0，则继续循环
                    waitTime = timeoutNanos - (System.nanoTime() - startTime);
                    if (waitTime <= 0) {
                        // 超过等待时间了，任务还没结束，返回false
                        return false;
                    }
                }
            }
        } finally {
            //退出方法前判断是否要给执行任务的线程添加中断标记
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
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

    /**
     * 同步方法，如果任务没有结束，那么会阻塞当前线程，直到任务结束或者被中断
     * 服务器和客户端经常会调用该方法同步等待结果
     *
     * @return
     * @throws InterruptedException
     */
    @Override
    public Promise<V> sync() throws InterruptedException {
        await();
        // 如果任务异常结束，则抛出异常
        rethrowIfFailed();
        return this;
    }

    @Override
    public Promise<V> syncUninterruptibly() {
        awaitUninterruptibly();
        rethrowIfFailed();
        return this;
    }

    /**
     * 获取任务执行结果，如果任务异常结束，则抛出异常
     */
    private void rethrowIfFailed() {
        Throwable cause = cause();
        if (cause == null) {
            return;
        }
        // 抛出异常,暂时先不从源码中引入该工具类
        //PlatformDependent.throwException(cause);
    }

    @Override
    public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        //检查监听器不为null
        checkNotNull(listener, "listener");
        //加锁
        synchronized (this) {
            //添加监听器
            addListener0(listener);
        }
        //判断任务是否完成，实际上就是检查result是否被赋值了
        if (isDone()) {
            // 如果任务已经完成，那么就需要立刻回调监听器，不然监听器里的逻辑就没机会被调用了；如果任务还没完成，那么监听器的回调方法会在任务结束时被调用
            notifyListeners();
        }

        return this;
    }

    private void addListener0(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listeners == null) {
            //listeners为null，则说明在这之前没有添加监听器，直接把该监听器赋值给属性即可
            listeners = listener;
        } else if (listeners instanceof DefaultFutureListeners) {
            //走到这里说明已经添加了多个监听器，监听器数组被包装在DefaultFutureListeners类中，所以要把监听器添加到数组中
            ((DefaultFutureListeners) listeners).add(listener);
        } else {
            //这种情况适用于第二次添加的时候，把第一次添加的监听器和本次添加的监听器传入DefaultFutureListeners的构造器函数中封装为一个监听器数组
            listeners = new DefaultFutureListeners((GenericFutureListener<? extends Future<? super V>>) listeners, listener);
        }
    }

    @Override
    public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        //检查监听器不为null
        checkNotNull(listeners, "listeners");
        //加锁
        synchronized (this) {
            //遍历传入的监听器，如果有其中任何一个为null，则停止循环
            for (GenericFutureListener<? extends Future<? super V>> listener : listeners) {
                if (listener == null) {
                    break;
                }
                //添加监听器
                addListener0(listener);
            }
        }
        if (isDone()) {
            // 如果任务已经完成，那么就需要立刻回调监听器，不然监听器里的逻辑就没 chance 被调了；如果任务还没完成，那么监听器的回调方法会在任务结束时被调用
            notifyListeners();
        }

        return this;
    }

    @Override
    public Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            removeListener0(listener);
        }

        return this;
    }

    private void removeListener0(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listeners instanceof DefaultFutureListeners) {
            //如果监听器是数组类型的，就从数组中删除
            ((DefaultFutureListeners) listeners).remove(listener);
        } else if (listeners == listener) {
            //如果监听器是单个监听器，就直接赋值为null
            listeners = null;
        }
    }

    @Override
    public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        checkNotNull(listeners, "listeners");
        synchronized (this) {
            for (GenericFutureListener<? extends Future<? super V>> listener : listeners) {
                if (listener == null) {
                    break;
                }
                removeListener0(listener);
            }
        }

        return this;
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

    @Override
    public String toString() {
        return toStringBuilder().toString();
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder buf = new StringBuilder(64)
                .append(StringUtil.simpleClassName(this))
                .append('@')
                .append(Integer.toHexString(hashCode()));

        Object result = this.result;
        if (result == SUCCESS) {
            buf.append("(success)");
        } else if (result == UNCANCELLABLE) {
            buf.append("(uncancellable)");
        } else if (result instanceof CauseHolder) {
            buf.append("(failure: ")
                    .append(((CauseHolder) result).cause)
                    .append(')');
        } else if (result != null) {
            buf.append("(success: ")
                    .append(result)
                    .append(')');
        } else {
            buf.append("(incomplete)");
        }

        return buf;
    }


}
