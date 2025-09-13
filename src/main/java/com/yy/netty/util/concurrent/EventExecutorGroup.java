package com.yy.netty.util.concurrent;

import java.util.concurrent.Executor;

/**
 * @Description:事件执行组，封装组的行为表现
 */
public interface EventExecutorGroup extends Executor {

    // 获取组内的下一个执行器
    EventExecutor next();

    // 优雅关闭(关闭组内的所有执行器)
    void shutdownGracefully();



}
