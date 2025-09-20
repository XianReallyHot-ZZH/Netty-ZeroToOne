package com.yy.netty.test.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Future<V> {

    /**
     * 尝试取消异步任务，如果任务已经完成或者取消，则返回false，否则进行取消并返回true
     *
     * @param mayInterruptIfRunning
     * @return
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 判断任务是否已经被取消
     *
     * @return
     */
    boolean isCancelled();

    /**
     * 判断任务是否已经完成
     *
     * @return
     */
    boolean isDone();

    /**
     * 获取任务结果
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * 获取任务结果，带有超时时间
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;


}
