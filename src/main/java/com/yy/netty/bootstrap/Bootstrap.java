package com.yy.netty.bootstrap;

import com.yy.netty.channel.*;
import com.yy.netty.util.concurrent.EventExecutor;
import com.yy.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @Description:客户端Socket网络搭建引导类，引导实现对SocketChannel的NIO事件处理,最终引导连接至某个ip：port，同时接受处理对服务端的IO读写
 */
public class Bootstrap<C extends Channel> {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private EventLoopGroup workerGroup;

    private volatile ChannelFactory<? extends Channel> channelFactory;

    public Bootstrap() {

    }

    public Bootstrap group(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        return this;
    }

    public Bootstrap channel(Class<? extends C> channelClass) {
        this.channelFactory = new ReflectiveChannelFactory<C>(channelClass);
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
        //这里的逻辑和serverbootstarp一样，但是在这里又要写一遍该方法，现在是不是发现，如果bootstarp和serverbootstarp有一个
        //抽象父类就好了，就可以在父类中定义模版方法了。实际上源码中确实有一个父类，这个方法被定义在父类中，但我们暂时还不引入
        //都是先生成channel，然后进行注册，然后进行“绑定”（服务端channel是绑定，客户端channel是连接）
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
                    channel.connect(remoteAddress,null, connectPromise);
                }
                //添加该监听器，如果channel连接失败，该监听器会关闭该channel
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }

    /**
     * 创建具体的channel实例并注册channel
     *
     * @return
     */
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        //在这里初始化服务端channel，反射创建对象调用的无参构造器，
        //可以去NioSocketChannel类中看看无参构造器中做了什么
        channel = channelFactory.newChannel();
        //这里是异步注册的，一般来说，workerGroup设置的也是一个线程执行器。只有在服务端的workerGroup中，才会设置多个线程执行器
        ChannelFuture regFuture = workerGroup.next().register(channel);
        return regFuture;
    }

    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        void registered() {
            registered = true;
        }

        @Override
        protected EventExecutor executor() {
            return super.executor();
        }
    }

}
