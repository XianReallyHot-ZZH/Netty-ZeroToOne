package com.yy.netty.test;

import com.yy.netty.bootstrap.ServerBootstrap;
import com.yy.netty.channel.Channel;
import com.yy.netty.channel.ChannelFuture;
import com.yy.netty.channel.ChannelOption;
import com.yy.netty.channel.nio.NioEventLoopGroup;
import com.yy.netty.channel.socket.nio.NioServerSocketChannel;
import com.yy.netty.util.concurrent.Future;
import com.yy.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;

public class ServerTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 创建服务端启动类
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // 创建两个Nio类型的事件循环EventLoopGroup
        // boss执行器组，负责处理accept事件
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // worker执行器组，负责处理read、write事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);


        ChannelFuture future = serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                //这里把服务端接受连接的数量设置为1，超过这个连接数应该就会报错。
                //是这个错误：java.net.SocketException: Connection reset by peer，
                //服务器接受的客户端连接超过了其设定最大值，就会关闭一些已经已经接受成功的连接
                //这里参数不能设置为0，在源码中会对option()的value进行判断：backlog < 1 ? 50 : backlog，传入的参数小于1就会使用默认配置50
                .option(ChannelOption.SO_BACKLOG, 1)
                .bind(8080)
                .addListener(new GenericFutureListener<Future<? super Object>>() {
                    @Override
                    public void operationComplete(Future<? super Object> future) {
                        // 绑定成功成功后，回调这里的监听逻辑
                        System.out.println("绑定成功，监听器逻辑也执行了！");
                    }
                }).sync();

        System.out.println("服务端启动成功！channelFuture result: " + future.getNow() + ", channel: " + future.channel());

    }


}
