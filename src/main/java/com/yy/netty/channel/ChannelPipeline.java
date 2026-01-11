package com.yy.netty.channel;

import java.util.Map;

/**
 * <p>
 * 可以看到ChannelPipeline接口继承了入站方法的接口和出站方法的接口，而我们知道ChannelPipeline只是个通道，或者说管道，
 * 在该通道中真正处理数据的是ChannelHandler，由此可以想到，该接口这样定义，肯定是在ChannelPipeline调用入站或者出站方法，而由每一个handler
 * 去处理，可想而知，ChannelHandler肯定也会通过一些方式，继承接口或者继承某些类，从而可以调用出站或入站方法
 * 所以在这个接口中，以add为开业的这类addxx方法，这些就是把ChannelHandler添加进ChannelPipeline的重要方法
 * </p>
 * 总结
 * 每一个channel对应一个ChannelPipeline
 * ChannelPipeline的角色是一个管道，管道的底层数据结构是链表，链表通过节点连接，节点中保存了ChannelHandler，节点的实现是ChannelHandlerContext，由节点来完成整个链路的串联
 * ChannelPipeline提供两类重要功能：
 * （1）将ChannelHandler添加进管道，即链表的维护与查询
 * （2）提供各种入站与出站功能的调用起点，即channel业务方法
 */
public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Map.Entry<String, ChannelHandler>> {

    // 该ChannelPipeline对应的 channel，一对一的关系
    Channel channel();

    // ----------------------------------- 链表的维护与查询 -----------------------------------
    // 添加ChannelHandler至链表的头部
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    // 添加ChannelHandler至链表的尾部
    ChannelPipeline addLast(String name, ChannelHandler handler);



    // ----------------------------------- 入站业务方法 返回值重写 -----------------------------------
    @Override
    ChannelPipeline fireChannelRead(Object msg);

    @Override
    ChannelPipeline fireChannelReadComplete();



    // ----------------------------------- 出站业务方法 返回值重写 -----------------------------------
    @Override
    ChannelPipeline flush();


}
