package com.yy.netty.test;

import com.yy.netty.bootstrap.Bootstrap;
import com.yy.netty.channel.ChannelFuture;
import com.yy.netty.channel.nio.NioEventLoopGroup;
import com.yy.netty.channel.socket.NioSocketChannel;

import java.io.IOException;

public class ClientTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 创建客户端Bootstrap引导类
        Bootstrap<NioSocketChannel> bootstrap = new Bootstrap<>();
        // 为客户端设置工作组
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        // 给引导类设置工作组
        ChannelFuture future = bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                // 连接至某个服务器，完成启动
                .connect("127.0.0.1", 8080);
        future.sync();
        System.out.println("客户端启动成功!channelFuture result: " + future.getNow());

    }

}
