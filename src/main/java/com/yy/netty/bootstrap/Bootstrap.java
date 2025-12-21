package com.yy.netty.bootstrap;

import com.yy.netty.channel.*;
import com.yy.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @Description:客户端Socket网络搭建引导类，引导实现对SocketChannel的NIO事件处理,最终引导连接至某个ip：port，同时接受处理对服务端的IO读写
 */
public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel> {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private final BootstrapConfig config = new BootstrapConfig(this);

    // 目标服务器地址
    private volatile SocketAddress remoteAddress;

    public Bootstrap() {

    }

    private Bootstrap(Bootstrap bootstrap) {
        super(bootstrap);
        remoteAddress = bootstrap.remoteAddress;
    }

    public Bootstrap remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    /**
     * 连接至目标服务器
     *
     * @param inetHost
     * @param inetPort
     */
    public ChannelFuture connect(String inetHost, int inetPort) {
        return connect(new InetSocketAddress(inetHost, inetPort));
    }

    private ChannelFuture connect(InetSocketAddress remoteAddress) {
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
        return doResolveAndConnect(remoteAddress, null);
    }

    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        //得到要注册的kehuduanchannel
        final Channel channel = regFuture.channel();
        if (regFuture.isDone()) {
            //这里的意思是future执行完成，但是没有成功，那么直接返回future即可
            if (!regFuture.isSuccess()) {
                return regFuture;
            }
            //完成的情况下，直接开始执行连接远程服务端的操作,首先创建一个future
            ChannelPromise promise = new DefaultChannelPromise(channel);
            return doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
        } else {
            //该内部类也是在抽象父类中，但这里我又在该类中定义了一遍
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // 注册失败了，那么就直接失败返回
                        promise.setFailure(cause);
                    } else {
                        // 注册成功，那么先标注一下注册成功了
                        promise.registered();
                        // 然后异步连接到远程服务端
                        doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    /**
     * 异步连接到远程服务端
     *
     * @param channel
     * @param remoteAddress
     * @param localAddress
     * @param promise
     * @return
     */
    private ChannelFuture doResolveAndConnect0(final Channel channel,
                                               final SocketAddress remoteAddress,
                                               final SocketAddress localAddress,
                                               final ChannelPromise promise) {
        try {
            //···
            //前面有一大段解析器解析远程地址的逻辑，在这里我删除了，那些不是重点，我们先关注重点
            doConnect(remoteAddress, localAddress, promise);
        } catch (Throwable cause) {
            promise.tryFailure(cause);
        }
        return promise;
    }

    /**
     * 异步连接到远程服务端的真正实现
     *
     * @param remoteAddress
     * @param localAddress
     * @param connectPromise
     */
    private static void doConnect(final SocketAddress remoteAddress,
                                  final SocketAddress localAddress,
                                  final ChannelPromise connectPromise) {
        //得到客户端的channel
        final Channel channel = connectPromise.channel();
        //异步实现connect
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (localAddress == null) {
                    //这里会走这个，我们并没有传递localAddress的地址
                    channel.connect(remoteAddress, null, connectPromise);
                }
                //添加该监听器，如果channel连接失败，该监听器会关闭该channel
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }

    @Override
    void init(Channel channel) throws Exception {
        // 得到父类中存储的所有参数项及其值
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            // 把初始化时用户配置的参数全都放到channel的config类中
            setChannelOptions(channel, options);
        }
    }

    @Override
    public Bootstrap validate() {
        super.validate();
        return this;
    }

    @Override
    public final BootstrapConfig config() {
        return config;
    }
}
