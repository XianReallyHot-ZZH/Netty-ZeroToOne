package com.yy.netty.bootstrap;

import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * @Description:服务端Socket网络搭建引导类，引导实现对ServerSocketChannel的NIO事件处理，最终实现服务端ip：port的绑定，同时接受各个客户端的连接请求和数据的io读取
 */
public class ServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrap.class);

    // 当前引导类负责引导的serverSocketChannel网络Channel对象
    private ServerSocketChannel serverSocketChannel;

    // 服务端 boss事件循环组,负责处理IO accept事件（连接事件）
    private EventLoopGroup bossGroup;

    // 服务端 work事件循环组, 负责处理IO read/write事件（读写事件）
    private EventLoopGroup workGroup;


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
     * 设置serverSocketChannel
     *
     * @param serverSocketChannel
     * @return
     */
    public ServerBootstrap serverSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
        return this;
    }


    /**
     * 为ServerSocketChannel绑定服务端口
     * 当绑定服务端口成功后，那么ServerSocketChannel就能接收客户端的连接请求了
     *
     * @param host
     * @param inetPort
     */
    public void bind(String host, int inetPort) {
        bind(new InetSocketAddress(host, inetPort));
    }

    private void bind(InetSocketAddress inetSocketAddress) {
        doBind(inetSocketAddress);
    }

    private void doBind(InetSocketAddress inetSocketAddress) {
        // 得到boss事件循环组中的事件执行器，也就是单线程执行器
        NioEventLoop nioEventLoop = (NioEventLoop) bossGroup.next().next();
        nioEventLoop.setServerSocketChannel(serverSocketChannel);
        nioEventLoop.setWorkGroup(workGroup);
        // 对服务端channel注册accept事件,在这里的第一个accept事件会顺便启动单线程
        nioEventLoop.register(serverSocketChannel, nioEventLoop);
        // channel进行端口绑定，注册是异步的实现，所以这里绑定也要成为异步实现，保证在单线程中注册accept事件要早于绑定端口行为
        doBind0(inetSocketAddress, nioEventLoop);

    }

    /**
     * @param inetSocketAddress
     * @param nioEventLoop
     * @Description:这里把绑定端口号封装成一个runnable，提交到单线程执行器的任务队列，绑定端口号仍然由单线程执行器完成,保证在单线程中注册accept事件要早于绑定端口行为
     */
    private void doBind0(InetSocketAddress inetSocketAddress, NioEventLoop nioEventLoop) {
        nioEventLoop.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocketChannel.bind(inetSocketAddress);
                    logger.info("服务端channel绑定至ip:port={}， 服务端启动成功！boss线程绑定至：{}",
                            inetSocketAddress.getAddress() + ":" + inetSocketAddress.getPort(), Thread.currentThread().getName());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });

    }

}
