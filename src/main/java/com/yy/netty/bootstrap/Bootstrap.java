package com.yy.netty.bootstrap;

import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @Description:客户端Socket网络搭建引导类，引导实现对SocketChannel的NIO事件处理,最终引导连接至某个ip：port，同时接受处理对服务端的IO读写
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private NioEventLoop nioEventLoop;

    private SocketChannel socketChannel;

    public Bootstrap() {

    }

    public Bootstrap group(NioEventLoopGroup workerGroup) {



        return this;
    }


    /**
     * 设置NIO事件处理器
     *
     * @param nioEventLoop
     * @return
     */
    public Bootstrap nioEventLoop(NioEventLoop nioEventLoop) {
        this.nioEventLoop = nioEventLoop;
        return this;
    }

    /**
     * 设置SocketChannel
     *
     * @param socketChannel
     * @return
     */
    public Bootstrap socketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        return this;
    }

    /**
     * 连接至目标服务器
     *
     * @param inetHost
     * @param inetPort
     */
    public void connect(String inetHost, int inetPort) {
        connect(new InetSocketAddress(inetHost, inetPort));
    }

    private void connect(InetSocketAddress inetSocketAddress) {
        doConnect(inetSocketAddress);
    }

    private void doConnect(InetSocketAddress inetSocketAddress) {
        //注册channel的connect事件任务,随着第一个任务的提交，内部的单线程会正式启动
        nioEventLoop.register(socketChannel, this.nioEventLoop);
        //然后再提交连接服务器任务
        doConnect0(inetSocketAddress);
    }

    private void doConnect0(InetSocketAddress inetSocketAddress) {
        nioEventLoop.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socketChannel.connect(inetSocketAddress);
                    logger.info("客户端channel连接至服务端成功！");
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }

}
