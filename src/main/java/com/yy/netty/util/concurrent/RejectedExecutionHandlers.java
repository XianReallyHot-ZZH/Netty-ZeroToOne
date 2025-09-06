package com.yy.netty.util.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 * @Description:创建拒绝策略处理器
 */
public class RejectedExecutionHandlers {

    /**
     * 拒绝策略处理器实例
     */
    private static final RejectedExecutionHandler REJECT = new RejectedExecutionHandler() {
        @Override
        public void rejected(Runnable task, SingleThreadEventExecutor executor) {
            throw new RejectedExecutionException("Task " + task + " rejected from " + executor);
        }
    };


    private RejectedExecutionHandlers() {

    }

    public static RejectedExecutionHandler reject() {
        return REJECT;
    }

}
