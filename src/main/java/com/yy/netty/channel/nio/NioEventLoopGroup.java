package com.yy.netty.channel.nio;

import com.yy.netty.channel.*;
import com.yy.netty.util.concurrent.EventExecutorChooserFactory;
import com.yy.netty.util.concurrent.RejectedExecutionHandler;
import com.yy.netty.util.concurrent.RejectedExecutionHandlers;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @Description:事件循环线程组，其实就是包含了一组单线程执行器的事件循环组。实际上，这个循环组并不处理任何事件，真正处理事件的仍是组内的每一个nioeventloop
 */
public class NioEventLoopGroup extends MultithreadEventLoopGroup {

    public NioEventLoopGroup() {
        this(0);
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads 组内的线程数
     */
    public NioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads 组内的线程数
     * @param executor 线程创建执行器
     */
    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads      组内的线程数
     * @param threadFactory 线程工厂
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads         组内的线程数
     * @param executor         线程创建执行器
     * @param selectorProvider SelectorProvider
     */
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads          组内的线程数
     * @param threadFactory     线程工厂
     * @param selectorProvider  selectorProvider
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
        this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads              组内的线程数
     * @param executor              线程创建执行器
     * @param selectorProvider      SelectorProvider
     * @param selectStrategyFactory 选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads                  组内的线程数
     * @param threadFactory             线程工厂
     * @param selectorProvider          selectorProvider
     * @param selectStrategyFactory     选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads                  组内的线程数
     * @param executor                  线程工厂
     * @param chooserFactory            执行器选择器工厂
     * @param selectorProvider          selectorProvider
     * @param selectStrategyFactory     选择策略工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor,
                             EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                RejectedExecutionHandlers.reject());
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads                  组内的线程数
     * @param executor                  线程工厂
     * @param chooserFactory            执行器选择器工厂
     * @param selectorProvider          selectorProvider
     * @param selectStrategyFactory     选择策略工厂
     * @param rejectedExecutionHandler  拒绝执行策略handler
     */
    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory,
                             final RejectedExecutionHandler rejectedExecutionHandler) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
    }

    /**
     * 创建一个事件循环线程组
     *
     * @param nThreads                  组内的线程数
     * @param executor                  线程工厂
     * @param chooserFactory            执行器选择器工厂
     * @param selectorProvider          selectorProvider
     * @param selectStrategyFactory     选择策略工厂
     * @param rejectedExecutionHandler  拒绝执行策略handler
     * @param taskQueueFactory          任务队列工厂
     */
    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                             final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory,
                             final RejectedExecutionHandler rejectedExecutionHandler,
                             final EventLoopTaskQueueFactory taskQueueFactory) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                rejectedExecutionHandler, taskQueueFactory);
    }


    /**
     * 创建一个事件循环线程组
     *
     * @param executor      线程创建执行器
     * @param args          SelectorProvider,selectStrategyFactory,RejectedExecutionHandler，EventLoopTaskQueueFactory
     * @return
     * @throws Exception
     */
    @Override
    protected EventLoop newChild(Executor executor, Object[] args) throws Exception {
        EventLoopTaskQueueFactory queueFactory = args.length == 4 ? (EventLoopTaskQueueFactory) args[3] : null;
        return new NioEventLoop(this, executor,
                (SelectorProvider) args[0],
                ((SelectStrategyFactory) args[1]).newSelectStrategy(),
                (RejectedExecutionHandler) args[2],
                queueFactory);
    }

}
