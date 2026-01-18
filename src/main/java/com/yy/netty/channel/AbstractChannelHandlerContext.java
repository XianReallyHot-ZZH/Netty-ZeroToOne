package com.yy.netty.channel;

import com.yy.netty.util.ResourceLeakHint;
import com.yy.netty.util.concurrent.EventExecutor;
import com.yy.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象的ChannelHandlerContext类，所有ChannelHandlerContext的子类都需要继承该类,提供作为pipeline链表节点的基础通用功能
 * ChannelPipeline类中定义了调用方法，然后会调用到AbstractChannelHandlerContext类中，在AbstractChannelHandlerContext中，交给真正做事的ChannelHandler去执行。
 */
public abstract class AbstractChannelHandlerContext implements ChannelHandlerContext, ResourceLeakHint {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChannelHandlerContext.class);

    // ChannelHandler添加到ChannelPipeline链表中时，会有一个添加状态
    private static final int ADD_PENDING = 1;
    // 只有状态为ADD_COMPLETE的handler才能处理数据
    private static final int ADD_COMPLETE = 2;
    private static final int REMOVE_COMPLETE = 3;
    // ChannelHandler添加链表后的初始状态
    private static final int INIT = 0;

    // （双向链表）当前节点在链表上的后一个节点
    volatile AbstractChannelHandlerContext next;
    // （双向链表）当前节点在链表上的前一个节点
    volatile AbstractChannelHandlerContext prev;

    // 当前AbstractChannelHandlerContext节点所归属的ChannelPipeline，ChannelPipeline通过AbstractChannelHandlerContext节点可以间接的得到每一个ChannelHandler
    private final DefaultChannelPipeline pipeline;

    // ChannelHandler所对应的名字
    private final String name;

    // 该值为false，ChannelHandler状态为ADD_PENDING的时候，也可以响应pipeline中的事件,该值为true表示只有ChannelHandler的状态为ADD_COMPLETE时，才能响应pipeline中的事件
    private final boolean ordered;

    // 这是个很有意思的属性，变量名称为执行掩码
    // 我们会向ChannelPipeline中添加很多handler，每个InBoundHandler都有channelRead，如果有的handler并不对read事件感兴趣，并没有自定义实现ChannelRead方法
    // 数据在链表中传递的时候，就要自动跳过该handler。这个掩码，就是表明该handler对哪个事件感兴趣的。用的是位运算相关的技巧。
    private final int executionMask;

    final EventExecutor executor;
    private ChannelFuture succeededFuture;

    // 把初始状态赋值给handlerState，handlerState属性就是ChannelHandler刚添加到链表时的状态
    private volatile int handlerState = INIT;


    /**
     * 用于ChannelHandlerContext在初始化的时候就为其封装的ChannelHandler计算出感兴趣的事件类型
     * @param clazz
     * @return
     */
    static int mask(Class<? extends ChannelHandler> clazz) {
        // TODO:
        return 0;
    }

    /**
     * 构造方法
     *
     * @param pipeline
     * @param executor
     * @param name
     * @param handlerClass  用户自定义的handler
     */
    AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name, Class<? extends ChannelHandler> handlerClass) {
        this.name = ObjectUtil.checkNotNull(name, "name");
        this.pipeline = pipeline;
        this.executor = executor;
        //channelHandlerContext中保存channelHandler的执行条件掩码（是什么类型的ChannelHandler,对什么事件感兴趣）
        this.executionMask = mask(handlerClass);
        // Its ordered if its driven by the EventLoop or the given Executor is an instanceof OrderedEventExecutor.
        ordered = executor == null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public Channel channel() {
        return pipeline.channel();
    }

    @Override
    public EventExecutor executor() {
        if (executor == null) {
            return channel().eventLoop();
        } else {
            return executor;
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isRemoved() {
        return handlerState == REMOVE_COMPLETE;
    }

    // ------------------------------ 入站相关方法 ----------------------------------

    /**
     * pipeline上ChannelRead的流程起点
     * 用途：我们写代码的时候，写一个rpc框架，消息中间件等等，肯定要用到handler的这个方法
     *
     * @param msg
     * @return
     */
    @Override
    public ChannelHandlerContext fireChannelRead(Object msg) {
        return null;
    }


    // ------------------------------ 出站相关方法 ----------------------------------


}
