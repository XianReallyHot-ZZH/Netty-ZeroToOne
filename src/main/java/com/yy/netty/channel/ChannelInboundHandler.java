package com.yy.netty.channel;

/**
 * 入站处理器的最基本接口，可以理解为都是对channel各种响应的具体处理。用户可以继承这个接口，然后实现各种方法，完成对channel各种响应的实现。是一种扩产设计。
 * 每一个ChannelHandler最终都会存储在各自的ChannelHandlerContext链路节点中，
 * 最终是在pipeline上通过链路的方式调用起每一个ChannelHandlerContext节点，进而调用起节点内置的ChannelHandler，完成channel各种响应的调用处理。
 */
public interface ChannelInboundHandler extends ChannelHandler {

    // 处理从channel中读取到的信息
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    // 处理channel信息读取完毕，响应逻辑
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

    // 处理Channel上的用户事件
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;

    // 响应channel注册成功
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;

    // 响应channel取消注册
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

    // 响应channel已激活
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    // 响应channel已取消激活
    void channelInactive(ChannelHandlerContext ctx) throws Exception;

    // 响应channel可写状态改变
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;


    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;


}
