package com.yy.netty.channel;

import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.util.concurrent.DefaultThreadFactory;
import com.yy.netty.util.concurrent.SingleThreadEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

/**
 * @Description:单线程事件循环，只要在netty中见到eventloop，就可以把该类视为线程类
 */
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

    protected SingleThreadEventLoop(Executor executor, EventLoopTaskQueueFactory queueFactory) {
        super(executor, queueFactory, new DefaultThreadFactory());
    }

    @Override
    protected boolean hasTasks() {
        return super.hasTasks();
    }

    /**
     * <核心方法>
     * 在这里把服务端channel绑定到单线程执行器上,实际上就是把channel注册到执行器中的selector上
     * </核心方法>
     *
     * @param channel
     * @param nioEventLoop
     */
    public void register(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交注册任务的时候，执行器终于开始执行了
            nioEventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register0(channel, nioEventLoop);
                    logger.info("服务端的channel的accept事件已注册到多路复用器上了！:{}",Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * <核心方法>
     * 在这里把客户端channel绑定到单线程执行器上,实际上就是把channel注册到执行器中的selector上
     * </核心方法>
     *
     * @param channel
     * @param nioEventLoop
     */
    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交注册任务的时候，执行器终于开始执行了
            nioEventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register0(channel, nioEventLoop);
                    logger.info("客户端的channel的read事件已注册到多路复用器上了！:{}",Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * <核心方法>
     * channel绑定读事件，这里就不用区分是服务端还是客户端，因为都是SocketChannel
     * </核心方法>
     *
     * @param channel
     * @param nioEventLoop
     */
    public void registerRead(SocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register00(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交注册任务的时候，执行器终于开始执行了
            nioEventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    register00(channel, nioEventLoop);
                    logger.info("channel的read事件已注册到多路复用器上了！:{}",Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * @Description:注册channel到selector上，注册的是读事件
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register00(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_READ);
        } catch (Exception e) {
            logger.error("客户端注册Channel的read事件到selector上失败！",  e);
        }
    }

    /**
     * @Description:客户端启动类调用该方法注册channel到selector上，注册的是连接事件
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register0(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            logger.error("客户端注册Channel的read事件到selector上失败！",  e);
        }
    }

    /**
     * @Description:服务端启动类调用该方法注册channel到selector上，注册的是接受事件
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register0(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            logger.error("服务端注册Channel的accept事件到selector上失败！",  e);
        }
    }


}
