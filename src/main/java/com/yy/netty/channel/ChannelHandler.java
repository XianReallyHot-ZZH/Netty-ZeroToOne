package com.yy.netty.channel;

import java.lang.annotation.*;

/**
 * ChannelHandler基类，所有ChannelHandler的子类都需要继承该类
 */
public interface ChannelHandler {

    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * ChannelHandler是否可以公用的注解，这要考虑到并发问题。
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {

    }

}
