package com.yy.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Description:作为抽象类，定义了get方法的模版让子类使用
 * @param <V>
 */
public abstract class AbstractFuture<V> implements Future<V> {

    @Override
    public V get() throws InterruptedException, ExecutionException {
        // 阻塞等待
        await();
        // 尝试获取任务执行过程中的异常
        Throwable cause = cause();
        if (cause == null) {
            // 任务没有异常，那么就返回任务正常结果，如果没有正常结果（比如对应void的默认成功SUCCESS结果），那么就返回null
            return getNow();
        }
        // 如果任务是主动取消的，那么抛出主动取消的异常
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        // 否则抛出任务执行过程中的异常
        throw new ExecutionException(cause);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //阻塞了用户设定的时间之后
        if (await(timeout, unit)) {
            // 走到这，说明任务结束了，结束有两种情况：1、任务正常结束，2、任务异常结束
            Throwable cause = cause();
            if (cause == null) {
                return getNow();
            }
            if (cause instanceof CancellationException) {
                throw (CancellationException) cause;
            }
            throw new ExecutionException(cause);
        }
        // 走到这里，说明任务没有结束，那么抛出超时异常
        throw new TimeoutException();
    }

}
