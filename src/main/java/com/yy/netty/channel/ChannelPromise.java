package com.yy.netty.channel;


import com.yy.netty.util.concurrent.Future;
import com.yy.netty.util.concurrent.GenericFutureListener;
import com.yy.netty.util.concurrent.Promise;

/**
 * 扩充ChannelFuture增加Promise接口 -> ChannelPromise
 * 其实就是赋予了ChannelFuture操作结果的能力
 */
public interface ChannelPromise extends ChannelFuture, Promise<Void> {

    @Override
    Channel channel();

    boolean trySuccess();

    ChannelPromise setSuccess();

    ChannelPromise unvoid();

    // ---------------------------------- 以下方法是为了重塑返回对象为ChannelPromise ----------------------------------
    @Override
    ChannelPromise setSuccess(Void result);

    @Override
    ChannelPromise setFailure(Throwable cause);

    @Override
    ChannelPromise sync() throws InterruptedException;

    @Override
    ChannelPromise syncUninterruptibly();

    @Override
    ChannelPromise awaitUninterruptibly();

    @Override
    ChannelPromise await() throws InterruptedException;

    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

}
