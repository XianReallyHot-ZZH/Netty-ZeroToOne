package com.yy.netty.channel;

/**
 * 入站处理器接口，可以理解为都是对channel各种响应的具体处理。用户可以继承这个接口，然后实现各种方法，完成对channel各种响应的实现。是一种扩产设计。
 * 每一个ChannelHandler最终都会存储在各自的ChannelHandlerContext链路节点中，
 * 最终是在pipeline上通过链路的方式调用起每一个ChannelHandlerContext节点，进而调用起节点内置的ChannelHandler，完成channel各种响应的调用处理。
 */
public interface ChannelInboundHandler extends ChannelHandler {

    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

}
