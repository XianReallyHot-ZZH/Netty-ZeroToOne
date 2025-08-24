package com.yy.netty.bootstrap;

import com.yy.netty.channel.nio.NioEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

/**
 * @Description:客户端Socket网络搭建引导类，引导实现对SocketChannel的NIO事件处理
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private NioEventLoop nioEventLoop;

    private SocketChannel socketChannel;

    public Bootstrap() {

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

    }


}
