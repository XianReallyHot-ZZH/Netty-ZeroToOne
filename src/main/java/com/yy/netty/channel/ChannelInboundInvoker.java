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

    /**
     * 触发链路上 channel writabilityChanged 的入站方法，响应channel可写状态改变结果
     *
     * @return
     */
    ChannelInboundInvoker fireChannelWritabilityChanged();

    /**
     * 触发链路上 channel registered 的入站方法，响应channel注册成功结果
     * @return
     */
    ChannelInboundInvoker fireChannelRegistered();

    /**
     * 触发链路上 channel unregistered 的入站方法，响应channel取消注册成功结果
     *
     * @return
     */
    ChannelInboundInvoker fireChannelUnregistered();

    /**
     * 触发链路上 channel active 的入站方法，响应channel激活成功结果
     * 比如服务端Channel完成对端口的绑定，那么说明服务端Channel是激活的
     * 比如客户端Channel完成对服务端的连接，那么说明客户端Channel是激活的
     *
     * @return
     */
    ChannelInboundInvoker fireChannelActive();

    /**
     * 触发链路上 channel inactive 的入站方法，响应channel取消激活成功结果
     * 比如服务端Channel取消对端口的绑定，那么说明服务端Channel是不激活的
     * 比如客户端Channel取消对服务端的连接，那么说明客户端Channel是不激活的
     *
     * @return
     */
    ChannelInboundInvoker fireChannelInactive();

    /**
     * 触发链路上 channel exceptionCaught 的入站方法，响应channel异常结果
     *
     * @param cause
     * @return
     */
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);

    /**
     * 触发链路上 channel userEventTriggered 的入站方法，响应channel用户事件结果
     *
     * @param event
     * @return
     */
    ChannelInboundInvoker fireUserEventTriggered(Object event);



}
