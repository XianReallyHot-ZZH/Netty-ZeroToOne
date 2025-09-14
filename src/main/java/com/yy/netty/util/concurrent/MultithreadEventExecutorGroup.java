package com.yy.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @Description:多线程事件执行组，是对AbstractEventExecutorGroup的进一步具体实现，内部会对一些具体执行器进行管理，完成一些时间执行器组接口的具体实现
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    // 内部管理着多个具体的执行器实现对象
    private final EventExecutor[] childrens;

    private final EventExecutorChooserFactory.EventExecutorChooser chooser;


    /**
     * 构造方法,根据入参构造出多线程事件执行对象组（多个执行器）和内部的其他辅助工具对象
     *
     * @param nThreads          线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param threadFactory     线程工厂
     * @param args              SelectorProvider,selectStrategyFactory,RejectedExecutionHandler
     */
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * 构造方法,根据入参构造出多线程事件执行对象组（多个执行器）和内部的其他辅助工具对象
     *
     * @param nThreads  线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param executor  线程创建执行器
     * @param args      SelectorProvider,selectStrategyFactory,RejectedExecutionHandler
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }

    /**
     * 构造方法,根据入参构造出多线程事件执行对象组（多个执行器）和内部的其他辅助工具对象
     *
     * @param nThreads          线程数，即执行器的个数，每个执行器都是一个单线程的试执行器
     * @param executor          线程创建执行器
     * @param chooserFactory    执行器选择器工厂
     * @param args              SelectorProvider,selectStrategyFactory,RejectedExecutionHandler，EventLoopTaskQueueFactory
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        childrens = new EventExecutor[nThreads];

        // 创建多个执行器
        for (int i = 0; i < nThreads; i++) {
            boolean success = false;
            try {
                //创建每一个线程执行器，这个方法在顶层NioEventLoopGroup中实现。
                childrens[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    //如果第一个线程执行器就没创建成功，剩下的方法都不会执行
                    //如果从第二个线程执行器开始，执行器没有创建成功，那么就会关闭之前创建好的线程执行器。
                    for (int j = 0; j < i; j++) {
                        childrens[j].shutdownGracefully();
                    }
                    for (int j = 0; j < i; j++) {
                        EventExecutor e = childrens[j];
                        try {
                            //判断正在关闭的执行器的状态，如果还没终止，就等待一些时间再终止
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            //给当前线程设置一个中断标志
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        this.chooser = chooserFactory.newChooser(childrens);
    }

    /**
     * 子类可选择继承改写
     *
     * @return
     */
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    /**
     * 创建执行器.交由子类实现，由子类决定创建具体什么类型的执行器
     *
     * @param executor      线程创建执行器
     * @param args          SelectorProvider,selectStrategyFactory,RejectedExecutionHandler，EventLoopTaskQueueFactory
     * @return
     * @throws Exception
     */
    protected abstract EventExecutor newChild(Executor executor, Object[] args) throws Exception;

    public final int executorCount() {
        return childrens.length;
    }

    @Override
    public void shutdownGracefully() {
        for (EventExecutor e : childrens) {
            e.shutdownGracefully();
        }
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

}
