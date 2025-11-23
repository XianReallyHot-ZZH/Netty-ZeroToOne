package com.yy.netty.channel;

import com.yy.netty.util.concurrent.RejectedExecutionHandler;
import com.yy.netty.util.concurrent.SingleThreadEventExecutor;
import com.yy.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 将channel注册到该EventLoop上
     * <核心方法>
     *
     * @param channel
     * @return
     */
    public ChannelFuture register(Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }

    /**
     * 其实这个方法是上面register(Channel channel)方法的一个封装，ChannelPromise里的channel成员就是上面方法的入参，要干的事情都是一样的
     *
     * @param promise
     * @return
     */
    @Override
    public ChannelFuture register(ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        // 具体channel会自动将自己注册到该单线程EventLoop中（其实就是将channel自己绑定到具体单线程EventLoop的Selector上）
        promise.channel().unsafe().register(this, promise);
        return promise;
    }


}
