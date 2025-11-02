package com.yy.netty.channel;

import com.yy.netty.util.concurrent.EventExecutorGroup;

/**
 * @Description:事件循环组接口
 */
public interface EventLoopGroup extends EventExecutorGroup {

    /**
     * 重塑next方法，将方法的返回类型改为EventLoop
     *
     * @return
     */
    @Override
    EventLoop next();

    /**
     * 将一个channel注册到本组中
     *
     * @param channel
     * @return
     */
    ChannelFuture register(Channel channel);

    /**
     * 其实这个方法是上面register(Channel channel)方法的一个封装，ChannelPromise里的channel成员就是上面方法的入参，要干的事情都是一样的
     *
     * @param promise
     * @return
     */
    ChannelFuture register(ChannelPromise promise);

}
