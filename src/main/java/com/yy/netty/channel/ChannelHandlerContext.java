package com.yy.netty.channel;

import com.yy.netty.util.Attribute;
import com.yy.netty.util.AttributeKey;
import com.yy.netty.util.AttributeMap;
import com.yy.netty.util.concurrent.EventExecutor;

/**
 * <p>
 * 该类是ChannelPipeline中包装handler的类，里面封装着ChannelHandler，和每一个handler的上下文信息
 * 而正是一个个ChannelHandlerContext对象，构成了ChannelPipeline中责任链的链表，链表的每一个节点都是ChannelHandlerContext
 * 对象，每一个ChannelHandlerContext对象里都有一个handler。这个接口也继承了出站入站方法,用来和链表的顶层结构channelPipeline进行互动
 * </p>
 * 同时也应该注意到该接口继承了AttributeMap接口，这说明ChannelHandlerContext的实现类本身也是一个map，那么用户存储在该实现类中的
 * 参数，在某些类中应该也是可以获得的。
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    // 当前节点属于哪个 pipeline
    ChannelPipeline pipeline();

    // 当前节点属于哪个 channel
    Channel channel();

    // 当前节点关联的事件执行器，其实就是channel关联的那个事件执行器
    EventExecutor executor();

    String name();

    // 当前节点持有的handler，ChannelHandler和ChannelHandlerContext是一对一的关系
    ChannelHandler handler();

    boolean isRemoved();

    // -------------------------------------- 入站方法 重写返回值 --------------------------------------
    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    // -------------------------------------- 出站方法 重写返回值 --------------------------------------
    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();


    // -------------------------------------- AttributeMap 方法 --------------------------------------
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);


}
