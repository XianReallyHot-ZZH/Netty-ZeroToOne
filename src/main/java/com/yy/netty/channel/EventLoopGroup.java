package com.yy.netty.channel;

import com.yy.netty.util.concurrent.EventExecutorGroup;

/**
 * @Description:事件循环组接口
 */
public interface EventLoopGroup extends EventExecutorGroup {

    /**
     * 重塑next方法，将方法的返回类型改为EventLoop
     *
     * @return
     */
    @Override
    EventLoop next();

}
