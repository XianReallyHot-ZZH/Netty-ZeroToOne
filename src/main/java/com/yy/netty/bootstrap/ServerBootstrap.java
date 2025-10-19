package com.yy.netty.bootstrap;

import com.yy.netty.channel.Channel;
import com.yy.netty.channel.ChannelFactory;
import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.channel.ReflectiveChannelFactory;
import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.util.concurrent.DefaultPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

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

    public DefaultPromise<Object> bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * 为ServerSocketChannel绑定服务端口
     * 当绑定服务端口成功后，那么ServerSocketChannel就能接收客户端的连接请求了
     *
     * @param host
     * @param inetPort
     * @return DefaultPromise对象，用于同步等待bind结果
     */
    public DefaultPromise<Object> bind(String host, int inetPort) {
        return bind(new InetSocketAddress(host, inetPort));
    }

    private DefaultPromise<Object> bind(InetSocketAddress inetSocketAddress) {
        return doBind(inetSocketAddress);
    }

    private DefaultPromise<Object> doBind(InetSocketAddress inetSocketAddress) {
        // 得到boss事件循环组中的事件执行器，也就是单线程执行器
        NioEventLoop nioEventLoop = (NioEventLoop) bossGroup.next().next();
        nioEventLoop.setServerSocketChannel(serverSocketChannel);
        nioEventLoop.setWorkGroup(workGroup);
        // 对服务端channel注册accept事件,在这里的第一个accept事件会顺便启动单线程
        nioEventLoop.register(serverSocketChannel, nioEventLoop);
        // 生成一个promise对象，用于返回结果
        DefaultPromise<Object> promise = new DefaultPromise<>(nioEventLoop);
        // channel进行端口绑定，注册是异步的实现，所以这里绑定也要成为异步实现，保证在单线程中注册accept事件要早于绑定端口行为
        doBind0(inetSocketAddress, nioEventLoop, promise);
        return promise;
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

}
