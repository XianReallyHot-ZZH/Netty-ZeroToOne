package com.yy.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * ChannelHandler关注的事件类
 */
public class ChannelHandlerMask {

    private static final Logger logger = LoggerFactory.getLogger(ChannelHandlerMask.class);

    /**
     * 表示不同事件的常量
     * 用单独的一位来表达某一种事件，这是位运算的基础，&可以用来判断是否包含，|可以用来追加事件，先取反再与可以用来减少事件
     */
    static final int MASK_EXCEPTION_CAUGHT = 1;
    static final int MASK_CHANNEL_REGISTERED = 1 << 1;
    static final int MASK_CHANNEL_UNREGISTERED = 1 << 2;
    static final int MASK_CHANNEL_ACTIVE = 1 << 3;
    static final int MASK_CHANNEL_INACTIVE = 1 << 4;
    static final int MASK_CHANNEL_READ = 1 << 5;
    static final int MASK_CHANNEL_READ_COMPLETE = 1 << 6;
    static final int MASK_USER_EVENT_TRIGGERED = 1 << 7;
    static final int MASK_CHANNEL_WRITABILITY_CHANGED = 1 << 8;
    static final int MASK_BIND = 1 << 9;
    static final int MASK_CONNECT = 1 << 10;
    static final int MASK_DISCONNECT = 1 << 11;
    static final int MASK_CLOSE = 1 << 12;
    static final int MASK_DEREGISTER = 1 << 13;
    static final int MASK_READ = 1 << 14;
    static final int MASK_WRITE = 1 << 15;
    static final int MASK_FLUSH = 1 << 16;

    /**
     * 入站处理器所拥有的所有常量事件，与运算做加法，相当于把所有事件加到一起
     * 这里用的就是 | 来做加法
     */
    private static final int MASK_ALL_INBOUND = MASK_EXCEPTION_CAUGHT | MASK_CHANNEL_REGISTERED |
            MASK_CHANNEL_UNREGISTERED | MASK_CHANNEL_ACTIVE | MASK_CHANNEL_INACTIVE | MASK_CHANNEL_READ |
            MASK_CHANNEL_READ_COMPLETE | MASK_USER_EVENT_TRIGGERED | MASK_CHANNEL_WRITABILITY_CHANGED;

    /**
     * 出站处理器所拥有的所有常量事件
     */
    private static final int MASK_ALL_OUTBOUND = MASK_EXCEPTION_CAUGHT | MASK_BIND | MASK_CONNECT | MASK_DISCONNECT |
            MASK_CLOSE | MASK_DEREGISTER | MASK_READ | MASK_WRITE | MASK_FLUSH;

    private static final ThreadLocal<Map<Class<? extends ChannelHandler>, Integer>> MASKS = new ThreadLocal<Map<Class<? extends ChannelHandler>, Integer>>() {
        @Override
        protected Map<Class<? extends ChannelHandler>, Integer> initialValue() {
            return new WeakHashMap<Class<? extends ChannelHandler>, Integer>(32);
        }
    };

    /**
     * 判断传入的ChannelHandler实现类关注了哪些事件（即用户自定义实现了哪些事件）
     *
     * @param clazz
     * @return
     */
    static int mask(Class<? extends ChannelHandler> clazz) {
        //得到当前线程对应的存储事件类型的map，key为ChannelHandler，value为其感兴趣的事件类型的总和
        Map<Class<? extends ChannelHandler>, Integer> cache = MASKS.get();
        Integer mask = cache.get(clazz);
        if (mask == null) {
            //如果为null，说明是第一次添加，那就计算出该handler感兴趣的事件类型
            mask = mask0(clazz);
            //还要添加到map中
            cache.put(clazz, mask);
        }
        return mask;
    }

    // 计算ChannelHandler感兴趣的事件类型
    private static int mask0(Class<? extends ChannelHandler> handlerType) {
        int mask = MASK_EXCEPTION_CAUGHT;
        try {
            //判断该handler是否继承自ChannelInboundHandler类或者实现了该接口，这一步可以判断该handler是入站处理器还是出站处理器
            if (ChannelInboundHandler.class.isAssignableFrom(handlerType)) {
                //如果该ChannelHandler是Inbound类型的，则先将inbound事件全部设置进掩码中
                // mask |= MASK_ALL_INBOUND就是 mask = mask | MASK_ALL_INBOUND
                mask |= MASK_ALL_INBOUND;
                //接下来就找看看该handler对那些时间不感兴趣，不感兴趣的，就从感兴趣的事件总和中除去
                //判断的标准就是产看该handler的每个方法上是否添加了@Skip注解，如果添加了该注解，则表示不感兴趣，那就用先取反然后&运算，把
                //该事件的值从事件总和中减去，具体逻辑可以去看看ChannelInboundHandlerAdapter类，该类中的所有方法都添加了@Skip注解
                //ChannelInboundHandlerAdapter的子类实现的方法没有@Skip注解，就表示该handler对特定事件感兴趣
                //每一个事件其实代表的就是handler中对应的方法是否可以被调用
                if (isSkippable(handlerType, "channelRegistered", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_REGISTERED;
                }
                if (isSkippable(handlerType, "channelUnregistered", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_UNREGISTERED;
                }
                if (isSkippable(handlerType, "channelActive", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_ACTIVE;
                }
                if (isSkippable(handlerType, "channelInactive", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_INACTIVE;
                }
                if (isSkippable(handlerType, "channelRead", ChannelHandlerContext.class, Object.class)) {
                    mask &= ~MASK_CHANNEL_READ;
                }
                if (isSkippable(handlerType, "channelReadComplete", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_READ_COMPLETE;
                }
                if (isSkippable(handlerType, "channelWritabilityChanged", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_WRITABILITY_CHANGED;
                }
                if (isSkippable(handlerType, "userEventTriggered", ChannelHandlerContext.class, Object.class)) {
                    mask &= ~MASK_USER_EVENT_TRIGGERED;
                }
            }
            //和上面逻辑相同，只不过变成了出站处理器
            if (ChannelOutboundHandler.class.isAssignableFrom(handlerType)) {
                mask |= MASK_ALL_OUTBOUND;
                if (isSkippable(handlerType, "bind", ChannelHandlerContext.class, SocketAddress.class, ChannelPromise.class)) {
                    mask &= ~MASK_BIND;
                }
                if (isSkippable(handlerType, "connect", ChannelHandlerContext.class, SocketAddress.class,
                        SocketAddress.class, ChannelPromise.class)) {
                    mask &= ~MASK_CONNECT;
                }
                if (isSkippable(handlerType, "disconnect", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_DISCONNECT;
                }
                if (isSkippable(handlerType, "close", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_CLOSE;
                }
                if (isSkippable(handlerType, "deregister", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_DEREGISTER;
                }
                if (isSkippable(handlerType, "read", ChannelHandlerContext.class)) {
                    mask &= ~MASK_READ;
                }
                if (isSkippable(handlerType, "write", ChannelHandlerContext.class,
                        Object.class, ChannelPromise.class)) {
                    mask &= ~MASK_WRITE;
                }
                if (isSkippable(handlerType, "flush", ChannelHandlerContext.class)) {
                    mask &= ~MASK_FLUSH;
                }
            }

            if (isSkippable(handlerType, "exceptionCaught", ChannelHandlerContext.class, Throwable.class)) {
                mask &= ~MASK_EXCEPTION_CAUGHT;
            }
        } catch (Exception e) {
            // Should never reach here.
            //PlatformDependent.throwException(e);
        }
        return mask;
    }

    // 判断该handler的特定方法是否被Skip注解
    private static boolean isSkippable(final Class<?> handlerType, final String methodName, final Class<?>... paramTypes) throws Exception {
        return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                Method m;
                try {
                    //判断该handler中是否实现了了对应事件的方法
                    m = handlerType.getMethod(methodName, paramTypes);
                } catch (NoSuchMethodException e) {
                    logger.debug("Class {} missing method {}, assume we can not skip execution", handlerType, methodName, e);
                    return false;
                }
                //该方法不为null并且方法上有@Skip注解，表明对此事件不感兴趣
                return m != null && m.isAnnotationPresent(Skip.class);
            }
        });
    }

    private ChannelHandlerMask() { }

    /**
     * 标注在ChannelHandler的方法上，用于区分是默认实现的还是用户实现的
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Skip {
        // no value
    }

}
