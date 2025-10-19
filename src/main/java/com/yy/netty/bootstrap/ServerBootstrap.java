package com.yy.netty.bootstrap;

import com.yy.netty.channel.*;
import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.util.concurrent.DefaultPromise;
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
        // 1、完成对指定channel的创建，这里其实就是服务端的channel了，然后将其注册到boss组中的单线程执行器的selector上，并指定感兴趣的事件，其实就是accept事件了
        final ChannelFuture regFuture = initAndRegister();
        // 2、得到创建的channel
        Channel channel = regFuture.channel();
        // 3、判断channel是否注册完成
        if (regFuture.isDone()) {
            // 服务端channel成功注册感兴趣的事件了，那么在本线程就可以继续为其绑定本地的服务端口了
            // 绑定的行为是异步的，所以创建一个ChannelFuture，用于协调调用方的线程
            DefaultChannelPromise promise = new DefaultChannelPromise(channel);
            // 执行异步绑定
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // 服务端channel还没有成功注册感兴趣的事件，那么绑定本地的服务端口的逻辑就需要挂载到regFuture的监听器上面，channel注册结束了，回调绑定端口的逻辑
            // 为了增加体现注册是否成功的信息，使用在该类定义的PendingRegistrationPromise，在DefaultChannelPromise的基础上添加注册是否成功的信息记录
            PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            // 在注册future上添加监听器
//            regFuture.addListener(new ChannelFutureListener() {})
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
    private void doBind0(final ChannelFuture regFuture,final Channel channel,final SocketAddress localAddress,final DefaultChannelPromise promise) {
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
    }

    /**
     * @param inetSocketAddress
     * @param nioEventLoop
     * @Description:这里把绑定端口号封装成一个runnable，提交到单线程执行器的任务队列，绑定端口号仍然由单线程执行器完成,保证在单线程中注册accept事件要早于绑定端口行为
     */
    private void doBind0(InetSocketAddress inetSocketAddress, NioEventLoop nioEventLoop, DefaultPromise<Object> promise) {
        nioEventLoop.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocketChannel.bind(inetSocketAddress);
                    logger.info("服务端channel绑定至ip:port={}， 服务端启动成功！boss线程绑定至：{}",
                            inetSocketAddress.getAddress() + ":" + inetSocketAddress.getPort(), Thread.currentThread().getName());

                    Thread.sleep(5000); // 这一行为了测试，记得注释掉
                    // bind成功，给promise设置成功结果，唤醒等待的线程
                    promise.setSuccess(null);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });

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
