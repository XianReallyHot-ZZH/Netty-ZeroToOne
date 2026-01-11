package com.yy.netty.channel;

/**
 * （链路能力的抽象）channel pipeline 链路上可以触发的入站类操作方法。
 * 什么是入站类操作方法呢？其实可以理解为是对channel的各种事件的响应，比如：channelRead，channelRegistered，channelActive
 */
public interface ChannelInboundInvoker {

    /**
     * 触发链路上 channel read 的入站方法
     * 其实就是触发链路上下一个节点的channel read方法
     *
     * @param msg
     * @return
     */
    ChannelInboundInvoker fireChannelRead(Object msg);

    /**
     * 触发链路上 channel readComplete 的入站方法
     * 其实就是触发链路上下一个节点的channel readComplete方法
     *
     * @return
     */
    ChannelInboundInvoker fireChannelReadComplete();

}
