package com.yy.netty.channel;

import java.net.SocketAddress;

/**
 * <p>
 * 含义同ChannelInboundInvoker，只不过这里处理的都是channel的出站事件。
 * 什么是出站事件呢？可以理解为是主动操作channel进行某种实现，比如：write，flush，bind，connect等等，是对channel的主动操作
 * </p>
 * <p>
 * 在netty中，很多接口定义了一些同名方法，这只是为了让某个类可以调用，但真正干活的是另一个类中的同名方法。
 * 就像NioEventLoopGroup和NioEventLoop那样，一个负责管理，一个负责真正执行
 * </p>
 */
public interface ChannelOutboundInvoker {

    ChannelFuture bind(SocketAddress localAddress);

    /**
     * 绑定本channel到本地服务端口(只有服务端的channel才会实现该方法)，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     * 服务端channel一定要实现该方法，客户端channel可以实现该方法（不实现可以）
     *
     * @param localAddress  本地服务端口
     * @param promise       和本次绑定逻辑相关的ChannelPromise协调器
     */
    ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise);

    ChannelFuture connect(SocketAddress remoteAddress);

    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise);

    /**
     * 连接本channel到远程服务端口(只有客户端的channel才会实现该方法)，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     *
     * @param remoteAddress
     * @param localAddress
     * @param promise
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

    ChannelFuture disconnect();

    ChannelFuture disconnect(ChannelPromise promise);

    /**
     * 关闭本channel
     *
     * @return
     */
    ChannelFuture close();

    ChannelFuture close(ChannelPromise promise);

    ChannelFuture deregister();

    ChannelFuture deregister(ChannelPromise promise);

    ChannelOutboundInvoker read();

    ChannelFuture write(Object msg);

    ChannelFuture write(Object msg, ChannelPromise promise);

    ChannelOutboundInvoker flush();

    ChannelFuture writeAndFlush(Object msg);

    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);

    ChannelPromise newPromise();

    ChannelFuture newSucceededFuture();

    ChannelFuture newFailedFuture(Throwable cause);

}
