package com.yy.netty.test;

import com.yy.netty.bootstrap.ServerBootstrap;
import com.yy.netty.channel.nio.NioEventLoopGroup;
import com.yy.netty.util.concurrent.Future;
import com.yy.netty.util.concurrent.GenericFutureListener;
import com.yy.netty.util.concurrent.Promise;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class ServerTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 创建服务端的socketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 创建服务端启动类
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // 创建两个Nio类型的事件循环EventLoopGroup
        // boss执行器组，负责处理accept事件
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // worker执行器组，负责处理read、write事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);
//        // 给服务端启动类设置工作组和serverSocketChannel
//        serverBootstrap.group(bossGroup, workerGroup).serverSocketChannel(serverSocketChannel);
//        // 绑定服务端的ip和端口，完成启动
//        serverBootstrap.bind("127.0.0.1", 8080);

        Promise<Object> promise = serverBootstrap
                .group(bossGroup, workerGroup)
                .serverSocketChannel(serverSocketChannel)
                .bind("127.0.0.1", 8080)
                .addListener(new GenericFutureListener<Future<? super Object>>() {
                    @Override
                    public void operationComplete(Future<? super Object> future) {
                        // 绑定成功成功后，回调这里的监听逻辑
                        System.out.println("绑定成功，监听器逻辑也执行了！");
                    }
                }).sync();

        System.out.println("服务端启动成功！promise result: " + promise.getNow());

    }


}
