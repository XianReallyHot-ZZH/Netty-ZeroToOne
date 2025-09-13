package com.yy.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @Description:事件执行器，封装单个事件执行器的行为表现
 */
public interface EventExecutor extends Executor {

    // 优雅关闭
    void shutdownGracefully();

    boolean isTerminated();

    void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException;

}
