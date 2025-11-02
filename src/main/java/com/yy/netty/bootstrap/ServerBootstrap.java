package com.yy.netty.bootstrap;

import com.yy.netty.channel.*;
import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.SocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @Description:服务端Socket网络搭建引导类，引导实现对ServerSocketChannel的NIO事件处理，最终实现服务端ip：port的绑定，同时接受各个客户端的连接请求和数据的io读取
 */
public class ServerBootstrap<C extends Channel> {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrap.class);

    // 服务端 boss事件循环组,负责处理IO accept事件（连接事件）
    private EventLoopGroup bossGroup;

    // 服务端 work事件循环组, 负责处理IO read/write事件（读写事件）
    private EventLoopGroup workGroup;

    // channel工厂，用于后续生产指定类型的channel实例
    private volatile ChannelFactory<? extends Channel> channelFactory;

    public ServerBootstrap() {

    }

    /**
     * 设置boss和work线程组
     *
     * @param bossGroup
     * @param workerGroup
     * @return
     */
    public ServerBootstrap group(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.bossGroup = bossGroup;
        this.workGroup = workerGroup;
        return this;
    }

    /**
     * 根据入参的channel类创建channel的工厂，用于后续生产指定类型的channel实例
     *
     * @param channelClazz
     * @return
     */
    public ServerBootstrap channel(Class<? extends C> channelClazz) {
        this.channelFactory = new ReflectiveChannelFactory<C>(channelClazz);
        return this;
    }

    /**
     * 为NioServerSocketChannel绑定服务端口
     *
     * @param inetPort
     * @return
     */
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * 为NioServerSocketChannel绑定服务端口
     * 当绑定服务端口成功后，那么ServerSocketChannel就能接收客户端的连接请求了
     *
     * @param inetHost
     * @param inetPort
     * @return DefaultPromise对象，用于同步等待bind结果
     */
    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }

    private ChannelFuture bind(InetSocketAddress localAddress) {
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    private ChannelFuture doBind(SocketAddress localAddress) {
        // 1、完成对指定channel的创建，这里其实就是服务端的channel了，然后将其注册到boss组中的单线程执行器的selector上（不带任务感兴趣事件的注册行为，其实就是为了将channel和一个EventLoop进行绑定），
        // 这里还没法成功注册accep事件，因为还没进行端口绑定，在下面绑定端口的逻辑里会真正为channel注册accept事件
        final ChannelFuture regFuture = initAndRegister();
        // 2、得到创建的channel
        Channel channel = regFuture.channel();
        // 3、判断channel是否注册完成
        if (regFuture.isDone()) {
            // 服务端channel成功注册到EventLoop了，那么在本线程就可以继续为其绑定本地的服务端口了
            // 绑定的行为是异步的，所以创建一个ChannelFuture，用于协调调用方的线程
            DefaultChannelPromise promise = new DefaultChannelPromise(channel);
            // 执行异步绑定，只有异步绑定成功后，本服务端channel才会真正完成accept事件注册，此时服务channel才真正能接受客户端的连接了
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // 服务端channel还没有成功注册感兴趣的事件，那么绑定本地的服务端口的逻辑就需要挂载到regFuture的监听器上面，channel注册结束了，回调绑定端口的逻辑
            // 为了协助判断服务端channel是否注册成功，使用在该类定义的PendingRegistrationPromise，在DefaultChannelPromise的基础上添加注册是否成功的信息记录
            PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            // 在注册future上添加监听器
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // 说明channel注册失败了
                        promise.setFailure(cause);
                    } else {
                        // 说明channel注册成功，那么开始绑定本地的服务端口了
                        promise.registered();
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    /**
     * 异步绑定服务端端口实现
     *
     * @param regFuture
     * @param channel
     * @param localAddress
     * @param promise
     */
    private void doBind0(final ChannelFuture regFuture, final Channel channel, final SocketAddress localAddress, final DefaultChannelPromise promise) {
        // 异步绑定
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //在这里仍要判断一次服务端的channel是否注册成功
                if (regFuture.isSuccess()) {
                    //注册成功之后开始绑定
                    channel.bind(localAddress, promise);
                } else {
                    //走到这里说明没有注册成功，把异常赋值给promise
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    /**
     * init: 完成对指定channel的创建，这里其实就是服务端的channel了，
     * register: 然后将channel注册到boss组中的单线程执行器的selector上，并为其指定感兴趣的事件，那这里其实就是accept事件了
     *
     * @return
     */
    private ChannelFuture initAndRegister() {
        Channel channel = null;
        //在这里初始化服务端channel，反射创建对象调用的无参构造器
        channel = channelFactory.newChannel();
        //这里是异步注册的，一般来说，bossGroup设置的都是一个线程。
        ChannelFuture regFuture = bossGroup.next().register(channel);
        return regFuture;
    }


    /**
     * 为了增加体现注册是否成功的信息，在DefaultChannelPromise的基础上添加注册是否成功的信息记录
     */
    static class PendingRegistrationPromise extends DefaultChannelPromise {

        private volatile boolean registered;

        public PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        //该方法是该静态类独有的,用于改变成员变量registered的值，该方法被调用的时候，registered赋值为true
        void registered() {
            registered = true;
        }

    }

}
