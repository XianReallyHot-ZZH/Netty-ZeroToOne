package com.yy.netty.channel;

import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.util.concurrent.RejectedExecutionHandler;
import com.yy.netty.util.concurrent.SingleThreadEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * @Description:单线程事件循环，只要在netty中见到eventloop，就可以把该类视为线程类
 */
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventLoop.class);

    //任务队列的容量，默认是Integer的最大值
    protected static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;


    /**
     * 构造方法
     *
     * @param parent                   当前单线程事件循环器所属的事件循环器组
     * @param executor                 创建线程的执行器,该单线程执行器中的线程就是由这个执行器创建而来
     * @param addTaskWakesUp
     * @param taskQueue                任务队列
     * @param rejectedExecutionHandler 拒绝策略
     */
    protected SingleThreadEventLoop(EventLoopGroup parent,
                                    Executor executor,
                                    boolean addTaskWakesUp,
                                    Queue<Runnable> taskQueue,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    }

    @Override
    public EventLoopGroup parent() {
        return null;
    }

    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    protected boolean hasTasks() {
        return super.hasTasks();
    }

    /**
     * <核心方法>
     * 在这里把服务端channel的accept事件绑定到单线程执行器上,实际上就是把channel注册到执行器中的selector上
     * 在第一次向单线程执行器中提交注册任务的时候，执行器的线程会被启动
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
                    logger.info("服务端的channel的accept事件已注册到多路复用器上了！ThreadName:{}", Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * <核心方法>
     * 在这里把客户端channel的connect事件绑定到单线程执行器上,实际上就是把channel注册到执行器中的selector上
     * 在第一次向单线程执行器中提交注册任务的时候，执行器的线程会被启动
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
                    logger.info("客户端的channel的connect事件已注册到多路复用器上了！ThreadName::{}", Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * <核心方法>
     * channel绑定读事件，这里就不用区分是服务端还是客户端，因为都是SocketChannel
     * 在第一次向单线程执行器中提交注册任务的时候，执行器的线程会被启动
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
                    logger.info("channel的read事件已注册到多路复用器上了！ThreadName::{}", Thread.currentThread().getName());
                }
            });
        }
    }

    /**
     * @param channel
     * @param nioEventLoop
     * @Description:注册channel到selector上，注册的是读事件
     */
    private void register00(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_READ);
        } catch (Exception e) {
            logger.error("客户端注册Channel的read事件到selector上失败！", e);
        }
    }

    /**
     * @param channel
     * @param nioEventLoop
     * @Description:客户端启动类调用该方法注册channel到selector上，注册的是连接事件
     */
    private void register0(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            logger.error("客户端注册Channel的read事件到selector上失败！", e);
        }
    }

    /**
     * @param channel
     * @param nioEventLoop
     * @Description:服务端启动类调用该方法注册channel到selector上，注册的是接受事件
     */
    private void register0(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            logger.error("服务端注册Channel的accept事件到selector上失败！", e);
        }
    }


}
