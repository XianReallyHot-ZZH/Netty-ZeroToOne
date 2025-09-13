package com.yy.netty.channel;

import com.yy.netty.util.NettyRuntime;
import com.yy.netty.util.concurrent.DefaultThreadFactory;
import com.yy.netty.util.concurrent.EventExecutorChooserFactory;
import com.yy.netty.util.concurrent.MultithreadEventExecutorGroup;
import com.yy.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    }

    protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

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
     * @param executor
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    protected abstract EventLoop newChild(Executor executor, Object[] args) throws Exception;
}
