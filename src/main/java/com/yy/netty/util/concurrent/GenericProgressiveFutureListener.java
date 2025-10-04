package com.yy.netty.util.concurrent;

/**
 * 通用的可查看进度future的监听器
 *
 * @param <F>
 */
public interface GenericProgressiveFutureListener<F extends ProgressiveFuture<?>> extends GenericFutureListener<F> {

    /**
     * 进度监听
     *
     * @param progressiveFuture
     * @param progress
     * @param total
     */
    void operationProgressed(F progressiveFuture, long progress, long total);

}
