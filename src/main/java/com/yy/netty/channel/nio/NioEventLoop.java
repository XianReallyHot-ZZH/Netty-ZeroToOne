package com.yy.netty.channel.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @Description:NIO类型事件循环器（执行器），nio中selector各种事件，都由该类处理
 */
public class NioEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

//    private final ServerSocketChannel serverSocketChannel;
//
//    private final SocketChannel socketChannel;

    private NioEventLoop worker;

    public NioEventLoop(ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {

    }

    public void setWorker(NioEventLoop worker) {
        this.worker = worker;
    }


}
