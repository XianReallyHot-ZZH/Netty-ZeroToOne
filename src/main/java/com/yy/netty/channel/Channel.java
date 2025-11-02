package com.yy.netty.channel;

import java.net.SocketAddress;

/**
 * netty channel的顶级接口，先引入一部分方法好了
 * netty channel体系的目的：
 * 1、增强原生NIO channel的功能
 * 2、进行各层抽象与实现后，达到复用部分代码实现的目的，同时留出给上层自定义的方法
 * 3、适配netty的总体规划
 * <p>
 * 知识补充：
 * 客户端侧socketChannel历经的IO流程（初始化流程）： reg OP_CONNECT -> doConnect -> response OP_CONNECT and reg OP_READ -> response read；
 * 总结：客户端的socketChannel要先后注册两种事件，OP_CONNECT，OP_READ
 * 服务端serverSocketChannel历经的IO流程（初始化流程）: reg OP_ACCEPT -> doBind -> response OP_ACCEPT -> accept socketChannel and socketChannel reg OP_READ；
 * 总结：服务端涉及两种socketChannel，分别是服务端serverSocketChannel和客户端socketChannel，但是每种channel只需要注册和响应处理一种事件，
 * 具体为serverSocketChannel只需要注册和响应OP_ACCEPT，客户端socketChannel直接注册和响应OP_READ即可
 * </p>
 */
public interface Channel {

    ChannelId id();

    // 获取当前channel绑定的EventLoop，每个EventLoop持有一个selector，channel只会注册到一个selector上，所以channel有且只会关联一个EventLoop
    EventLoop eventLoop();

    // 只有服务端的socketChannel才会有parent（ServerSocketChannel）
    Channel parent();

    /**
     * 判断nio的channel是否已经打开
     *
     * @return
     */
    boolean isOpen();

    /**
     * 判断nio的channel是否已经激活
     * 客户端Channel激活标准：1、channel已经打开；2、channel已经完成了connect（连接远程）
     * 服务端Channel激活标准：1、channel已经打开；2、channel已经绑定到了本地端口
     *
     * @return
     */
    boolean isActive();

    /**
     * 判断本channel是否已经注册到EventLoop中(绑定到了Selector上)
     *
     * @return
     */
    boolean isRegistered();

    /**
     * 本channel绑定的本地端口
     *
     * @return
     */
    SocketAddress localAddress();

    /**
     * 本channel绑定的远程端口
     *
     * @return
     */
    SocketAddress remoteAddress();

    /**
     * 返回一个关联close流程的ChannelFuture，在不同线程间协调close的结果
     *
     * @return
     */
    ChannelFuture closeFuture();

    /**
     * 绑定本channel到本地服务端口(只有服务端的channel才会实现该方法)，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     * 服务端channel一定要实现该方法，客户端channel可以实现该方法（不实现可以）
     *
     * @param localAddress  本地服务端口
     * @param promise       和本次绑定逻辑相关的ChannelPromise协调器
     */
    void bind(SocketAddress localAddress, ChannelPromise promise);

    /**
     * 连接本channel到远程服务端口(只有客户端的channel才会实现该方法)，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     *
     * @param remoteAddress
     * @param localAddress
     * @param promise
     */
    void connect(SocketAddress remoteAddress, final SocketAddress localAddress, ChannelPromise promise);

    /**
     * 注册本channel到指定的EventLoop（完成channel到Selector的绑定），该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     * 服务端channel和客户端channel都要实现该方法,两种channel的初始化流程种会有这一步
     *
     * @param eventLoop     指定的EventLoop
     * @param promise       和本次注册逻辑相关的ChannelPromise协调器
     */
    void register(EventLoop eventLoop, ChannelPromise promise);

    /**
     * 为本channel注册读事件，该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     * 这地方的读是一种抽象的概念，具体不同的channel关注的读事件是不一样的
     * 服务端channel的“读”事件：OP_ACCEPT
     * 客户端channel的“读”事件：OP_READ
     */
    void beginRead();

}
