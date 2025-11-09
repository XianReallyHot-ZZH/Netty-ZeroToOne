package com.yy.netty.channel;

import com.yy.netty.util.concurrent.*;
import com.yy.netty.util.internal.ObjectUtil;

/**
 * ChannelPromise的默认实现，用于channel操作相关场景的线程协调器默认实现，赋予future和promise的能力
 */
public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise {

    private final Channel channel;

    // 关联一个channel的构造方法
    public DefaultChannelPromise(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
    }

    // 关联一个channel和执行器的构造方法
    public DefaultChannelPromise(Channel channel, EventExecutor executor) {
        super(executor);
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
    }

    @Override
    public Channel channel() {
        return this.channel;
    }

    // 这个方法目前还没用到，先写在这里
    @Override
    public boolean isVoid() {
        return false;
    }

    // 这个方法目前还没用到，先写在这里
    @Override
    public ChannelPromise unvoid() {
        return this;
    }

    @Override
    public boolean trySuccess() {
        return super.trySuccess(null);
    }

    @Override
    public ChannelPromise setSuccess() {
        super.setSuccess(null);
        return this;
    }

    @Override
    protected EventExecutor executor() {
        EventExecutor e = super.executor();
        if (e == null) {
            return channel().eventLoop();
        } else {
            return e;
        }
    }

    @Override
    protected void checkDeadLock() {
        if (channel().isRegistered()) {
            super.checkDeadLock();
        }
    }

    @Override
    public ChannelPromise setSuccess(Void result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public ChannelPromise setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }

    @Override
    public ChannelPromise await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ChannelPromise awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public ChannelPromise sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ChannelPromise syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
         super.removeListener(listener);
         return this;
    }

    @Override
    public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }
}
