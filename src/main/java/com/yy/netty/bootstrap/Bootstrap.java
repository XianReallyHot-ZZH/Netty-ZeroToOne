package com.yy.netty.bootstrap;

import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.channel.nio.NioEventLoop;
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

    private EventLoopGroup workerGroup;

    public Bootstrap() {

    }

    public Bootstrap group(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        return this;
    }

    /**
     * 设置SocketChannel
     * 注意：这个方法有点并发问题，调用该方法时为了在doConnect方法中拿到要处理的channel对象，并发时是有问题的，先不处理了
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
        //获得单线程执行器
        nioEventLoop = (NioEventLoop) workerGroup.next().next();
        nioEventLoop.setSocketChannel(socketChannel);
        //注册channel的connect事件任务,随着第一个任务的提交，内部的单线程会正式启动
        nioEventLoop.register(socketChannel, this.nioEventLoop);
        //然后再提交连接服务器任务
        doConnect0(inetSocketAddress, nioEventLoop);
    }

    private void doConnect0(InetSocketAddress inetSocketAddress, NioEventLoop nioEventLoop) {
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
