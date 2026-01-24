package com.yy.netty.channel;

import java.net.SocketAddress;

/*
 * 出站处理器的最基本接口，可以理解为是对channel的各种主动操作。用户可以继承这个接口，然后实现各种方法，完成对channel具体操作的实现。是一种扩产设计。
 * 每一个ChannelHandler最终都会存储在各自的ChannelHandlerContext链路节点中，
 * 最终是在pipeline上通过链路的方式调用起每一个ChannelHandlerContext节点，进而调用起节点内置的ChannelHandler，完成channel各种响应的调用处理。
 */
public interface ChannelOutboundHandler extends ChannelHandler {

    // 开启本channel的‘读’功能，比如服务端就是监听客户端的连接请求，客户端就是监听服务端的响应请求
    void read(ChannelHandlerContext ctx) throws Exception;

    // 绑定本channel到本地服务端口(只有服务端的channel才会实现该方法)
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;

    // 将信息写进本channel的写缓存区
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;

    // 将信息从本channel的写缓存区写入到channel中，进而发送给对端
    void flush(ChannelHandlerContext ctx) throws Exception;

    // 将本channel连接到指定节点
    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception;

    // 断开本channel与对端的连接
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    // 关闭本channel
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    // 反注册本channel
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;
}
