package com.yy.netty.test;

import com.yy.netty.bootstrap.Bootstrap;
import com.yy.netty.channel.nio.NioEventLoop;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientTest {

    public static void main(String[] args) throws IOException {
        // 创建客户端的SocketChannel
        SocketChannel socketChannel = SocketChannel.open();
        // 创建客户端Bootstrap引导类
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoop boss = new NioEventLoop(null, socketChannel);
        NioEventLoop worker = new NioEventLoop(null, socketChannel);
        boss.setWorker(worker);
        // 给客户端启动类设置nioEventLoop执行器和客户端端socketChannel
        bootstrap.nioEventLoop(boss).socketChannel(socketChannel);
        // 连接至某个服务器，完成启动
        bootstrap.connect("127.0.0.1", 8080);

    }

}
