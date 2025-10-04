package com.yy.netty.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @Description:netty中重写了该接口，添加了一些重要的方法
 * @param <V>
 */
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * 判断任务是否成功
     *
     * @return
     */
    boolean isSuccess();

    /**
     * 判断任务是否可取消
     *
     * @return
     */
    boolean isCancellable();

    /**
     * 获取任务失败原因,对应任务执行时的异常
     *
     * @return
     */
    Throwable cause();

    /**
     * 无限同步等待，等待线程支持抛出InterruptedException
     *
     * @return
     * @throws InterruptedException
     */
    Future<V> sync() throws InterruptedException;

    /**
     * 无限同步等待，等待线程不支持抛出InterruptedException
     *
     * @return
     */
    Future<V> syncUninterruptibly();

    /**
     * 无限同步阻塞等待，等待线程支持中断抛出
     *
     * @return
     * @throws InterruptedException
     */
    Future<V> await() throws InterruptedException;

    /**
     * 无限同步阻塞等待，等待线程不支持中断抛出
     *
     * @return
     */
    Future<V> awaitUninterruptibly();

    /**
     * 有限同步阻塞等待，等待线程支持中断抛出
     * 返回结果：true:任务完成done(任务正常完成、任务异常结束都算done)，false:任务未完成
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 有限同步阻塞等待，等待线程支持中断抛出
     * 返回结果：true:任务完成done，false:任务未完成
     *
     * @param timeoutMillis
     * @return
     * @throws InterruptedException
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * 有限阻塞等待，等待线程支持中断抛出
     * 返回结果：true:任务完成done，false:任务未完成
     *
     * @param timeout
     * @param unit
     * @return
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * 有限阻塞等待，等待线程支持中断抛出
     * 返回结果：true:任务完成done，false:任务未完成
     *
     * @param timeoutMillis
     * @return
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * 获取当前任务的正常业务结果，如果没有正常业务结果，则返回null
     *
     * @return
     */
    V getNow();

    /**
     * 取消任务
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     * @return
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 添加监听器
     *
     * @param listener
     * @return
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * 添加多个监听器
     *
     * @param listeners
     * @return
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * 移除监听器
     *
     * @param listener
     * @return
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * 移除多个监听器
     *
     * @param listeners
     * @return
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);


}
