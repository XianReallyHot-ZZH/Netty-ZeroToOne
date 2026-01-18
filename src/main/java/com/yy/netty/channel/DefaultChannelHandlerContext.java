package com.yy.netty.channel;

import com.yy.netty.util.concurrent.EventExecutor;

/**
 * 默认的ChannelHandlerContext, 以组合的方式，内部持有一个ChannelHandler
 */
final class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    // 内部持有一个ChannelHandler，组合的方式
    private final ChannelHandler handler;

    /**
     * 构造方法
     *
     * @param pipeline
     * @param executor
     * @param name
     * @param handler   外部传入的ChannelHandler
     */
    DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
        super(pipeline, executor, name, handler.getClass());
        this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }
}
