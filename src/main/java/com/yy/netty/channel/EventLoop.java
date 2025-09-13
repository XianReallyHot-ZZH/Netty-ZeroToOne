package com.yy.netty.channel;

import com.yy.netty.util.concurrent.EventExecutor;

/**
 * @Description:事件循环的接口,本身也是一个事件执行器，然后EventLoop的行为有一部分是和EventLoopGroup一致的，EventLoop可以看做是一个特殊的EventLoopGroup，那么我们EventLoop就直接继承EventLoopGroup了
 */
public interface EventLoop extends EventExecutor, EventLoopGroup {
}
