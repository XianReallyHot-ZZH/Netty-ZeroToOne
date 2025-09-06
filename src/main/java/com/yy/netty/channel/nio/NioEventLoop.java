package com.yy.netty.channel.nio;

import com.yy.netty.channel.SingleThreadEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @Description:NIO类型事件循环器（执行器），nio中selector各种事件，都由该类处理
 */
public class NioEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    private final ServerSocketChannel serverSocketChannel;

    private final SocketChannel socketChannel;

    // 多路复用选择器，一个NIO事件循环器对应一个多路复用选择器
    private final Selector selector;

    // 多路复用选择器的提供者
    private final SelectorProvider selectorProvider;

    // 对应服务端的IO读eventLoop，这个这样写不好，但是先就这样处理了
    private NioEventLoop worker;

    public NioEventLoop(ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {

    }

    public void setWorker(NioEventLoop worker) {
        this.worker = worker;
    }


    /**
     * <核心方法>
     *     在这里定义NioEventLoop的循环逻辑
     * </核心方法>
     */
    @Override
    protected void run() {

    }


    public Selector unwrappedSelector() {
        return selector;
    }
}
