package com.yy.netty.test.future;

/**
 * 本身是个runnable的Future
 *
 * @param <V>
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {

    @Override
    void run();

}
