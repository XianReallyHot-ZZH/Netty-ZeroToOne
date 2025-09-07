package com.yy.netty.test;

import com.yy.netty.bootstrap.Bootstrap;
import com.yy.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientTest {

    public static void main(String[] args) throws IOException {
        // 创建客户端的SocketChannel
        SocketChannel socketChannel = SocketChannel.open();
        // 创建客户端Bootstrap引导类
        Bootstrap bootstrap = new Bootstrap();
        // 为客户端设置工作组
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        // 给引导类设置工作组
        bootstrap.group(workerGroup).socketChannel(socketChannel);
        // 连接至某个服务器，完成启动
        bootstrap.connect("127.0.0.1", 8080);

    }

}
