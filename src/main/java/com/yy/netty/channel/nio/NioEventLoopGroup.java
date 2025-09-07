package com.yy.netty.channel.nio;

/**
 * @Description:线程组，其实就是包含了一组单线程执行器的事件循环组。实际上，这个循环组并不处理任何事件，真正处理事件的仍是nioeventloop
 */
public class NioEventLoopGroup {

    public NioEventLoopGroup() {
        this(0);
    }

    public NioEventLoopGroup(int nThreads) {

    }

}
