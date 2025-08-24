package com.yy.netty.test;

import com.yy.netty.bootstrap.ServerBootstrap;
import com.yy.netty.channel.nio.NioEventLoop;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class ServerTest {

    public static void main(String[] args) throws IOException {
        // 创建服务端的socketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 创建服务端启动类
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // 创建两个Nio类型的事件循环EventLoop（异步工作执行器）
        // boss执行器，负责处理accept事件
        NioEventLoop boss = new NioEventLoop(serverSocketChannel, null);
        // worker执行器，负责处理read、write事件
        NioEventLoop worker = new NioEventLoop(serverSocketChannel, null);
        //把worker执行器设置到boss执行器中，这样在boss执行器中接收到客户端连接，可以立刻提交给worker执行器
        boss.setWorker(worker);
        // 给服务端启动类设置nioEventLoop执行器和服务端socketChannel
        serverBootstrap.nioEventLoop(boss).serverSocketChannel(serverSocketChannel);
        // 绑定服务端的ip和端口，完成启动
        serverBootstrap.bind("127.0.0.1",8080);

    }


}
