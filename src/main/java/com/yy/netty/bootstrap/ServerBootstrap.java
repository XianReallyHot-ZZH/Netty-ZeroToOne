package com.yy.netty.bootstrap;

import com.yy.netty.channel.nio.NioEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ServerSocketChannel;

/**
 * @Description:服务端Socket网络搭建引导类，引导实现对ServerSocketChannel的NIO事件处理
 */
public class ServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrap.class);

    // 当前引导类负责引导的nio类型事件循环器（执行器）
    private NioEventLoop nioEventLoop;

    // 当前引导类负责引导的serverSocketChannel网络Channel对象
    private ServerSocketChannel serverSocketChannel;

    public ServerBootstrap() {

    }

    /**
     * 设置nioEventLoop
     *
     * @param nioEventLoop
     * @return
     */
    public ServerBootstrap nioEventLoop(NioEventLoop nioEventLoop) {
        this.nioEventLoop = nioEventLoop;
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
    }
}
