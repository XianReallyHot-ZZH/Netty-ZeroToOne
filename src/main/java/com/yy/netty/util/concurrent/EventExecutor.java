package com.yy.netty.util.concurrent;

/**
 * @Description:事件执行器，封装单个事件执行器的行为表现，EventExecutor的行为有一部分是和EventExecutorGroup一致的，然后EventExecutor可以看做是一个特殊的EventExecutorGroup，那么索性EventExecutor就继承EventExecutorGroup得了
 */
public interface EventExecutor extends EventExecutorGroup {

    @Override
    EventExecutor next();

}
