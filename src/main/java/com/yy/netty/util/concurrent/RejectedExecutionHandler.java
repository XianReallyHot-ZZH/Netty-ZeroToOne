package com.yy.netty.util.concurrent;

/**
 * 拒绝策略处理器
 */
public interface RejectedExecutionHandler {
    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
