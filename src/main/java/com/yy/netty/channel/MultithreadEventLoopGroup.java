package com.yy.netty.channel;

import com.yy.netty.util.NettyRuntime;
import com.yy.netty.util.concurrent.DefaultThreadFactory;
import com.yy.netty.util.concurrent.EventExecutorChooserFactory;
import com.yy.netty.util.concurrent.MultithreadEventExecutorGroup;
import com.yy.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @Description:多线程事件循环组，继承MultithreadEventExecutorGroup，重塑具象化某些父类的方法返回类型以从EventExecutor纠正到EventLoop，并实现部分父类方法
 */
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    }

    /**
     * 构造方法
     *
     * @param nThreads          线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param threadFactory     线程工厂
     * @param args              SelectorProvider,selectStrategyFactory,RejectedExecutionHandler
     */
    protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    /**
     * 构造方法
     *
     * @param nThreads      线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param executor      线程创建执行器
     * @param args          SelectorProvider,selectStrategyFactory,RejectedExecutionHandler
     */
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

    /**
     * 构造方法
     *
     * @param nThreads          线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param executor          线程创建执行器
     * @param chooserFactory    执行器选择器工厂
     * @param args              SelectorProvider,selectStrategyFactory,RejectedExecutionHandler，EventLoopTaskQueueFactory
     */
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
    }


    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    /**
     * 重塑抽象方法的返回，指向更具体的子类,整个方法需要由子类实现
     *
     * @param executor      线程创建执行器
     * @param args          SelectorProvider,selectStrategyFactory,RejectedExecutionHandler，EventLoopTaskQueueFactory
     * @return
     * @throws Exception
     */
    @Override
    protected abstract EventLoop newChild(Executor executor, Object[] args) throws Exception;

    /**
     * 将channel注册到本EventLoopGroup中
     *
     * @param channel
     * @return
     */
    @Override
    public ChannelFuture register(Channel channel) {
        // 选一个eventLoop进行绑定，其实目的就是将一个channel绑定到一个具体的EventLoop上（EventLoop里的Selector）
        return next().register(channel);
    }

    /**
     * 其实这个方法是上面register(Channel channel)方法的一个封装，ChannelPromise里的channel成员就是上面方法的入参，要干的事情都是一样的
     *
     * @param promise
     * @return
     */
    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }

}
