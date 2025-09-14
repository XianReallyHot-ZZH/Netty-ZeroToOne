package com.yy.netty.util.concurrent;

/**
 * @Description:事件执行器，封装单个事件执行器的行为表现，EventExecutor的行为有一部分是和EventExecutorGroup一致的，然后EventExecutor可以看做是一个特殊的EventExecutorGroup，那么索性EventExecutor就继承EventExecutorGroup得了
 */
public interface EventExecutor extends EventExecutorGroup {

    @Override
    EventExecutor next();

    // 当前执行器所归属的EventExecutorGroup
    EventExecutorGroup parent();

    // 判断传入的线程是否是当前执行器的线程
    boolean inEventLoop(Thread thread);

}
