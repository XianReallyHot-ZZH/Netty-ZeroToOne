package com.yy.netty.channel;

import com.yy.netty.util.concurrent.Future;
import com.yy.netty.util.concurrent.GenericFutureListener;

/**
 * channel的Future接口，用于协调异步操作结果,在使用该接口的场景会更聚焦于操作channel相关功能的逻辑
 */
public interface ChannelFuture extends Future<Void> {

    // 返回绑定在该future上的channel
    Channel channel();

    boolean isVoid();

    // -------------------------------------- 以下八个方法是为了 重塑返回对象为ChannelFuture --------------------------------------
    @Override
    ChannelFuture sync() throws InterruptedException;

    @Override
    ChannelFuture syncUninterruptibly();

    @Override
    ChannelFuture awaitUninterruptibly();

    @Override
    ChannelFuture await() throws InterruptedException;

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

}
