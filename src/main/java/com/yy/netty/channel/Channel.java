package com.yy.netty.channel;

import java.net.SocketAddress;

/**
 * netty channel的顶级接口，先引入一部分方法好了
 * netty channel体系的目的：
 * 1、增强原生NIO channel的功能
 * 2、进行各层抽象与实现后，达到复用部分代码实现的目的，同时留出给上层自定义的方法
 * 3、适配netty的总体规划
 */
public interface Channel {

    // 获取当前channel绑定的EventLoop，每个EventLoop持有一个selector，channel只会注册到一个selector上，所以channel有且只会关联一个EventLoop
    EventLoop eventLoop();

    /**
     * 绑定本地服务端口(只有服务端的channel才会实现该方法)，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     *
     * @param localAddress  本地服务端口
     * @param promise       和本次绑定逻辑相关的ChannelPromise协调器
     */
    void bind(SocketAddress localAddress, ChannelPromise promise);

}
