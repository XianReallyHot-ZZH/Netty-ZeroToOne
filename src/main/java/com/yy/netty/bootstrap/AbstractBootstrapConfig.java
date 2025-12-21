package com.yy.netty.bootstrap;


import com.yy.netty.channel.Channel;
import com.yy.netty.channel.ChannelFactory;
import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.StringUtil;

import java.net.SocketAddress;

/**
 * 抽象引导类的配置类
 *
 * @param <B>
 * @param <C>
 */
public abstract class AbstractBootstrapConfig<B extends AbstractBootstrap<B, C>, C extends Channel> {

    /**
     * 引导类
     */
    protected final B bootstrap;

    protected AbstractBootstrapConfig(B bootstrap) {
        this.bootstrap = ObjectUtil.checkNotNull(bootstrap, "bootstrap");
    }

    /**
     * 获取引导类里的本地地址
     *
     * @return
     */
    public final SocketAddress localAddress() {
        return bootstrap.localAddress();
    }

    /**
     * 获取引导类里的channel工厂
     *
     * @return
     */
    public final ChannelFactory<? extends C> channelFactory() {
        return bootstrap.channelFactory();
    }

    /**
     * 获取引导类里的事件循环组
     *
     * @return
     */
    public final EventLoopGroup group() {
        return bootstrap.group();
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
                .append(StringUtil.simpleClassName(this))
                .append('(');
        EventLoopGroup group = group();
        if (group != null) {
            buf.append("group: ")
                    .append(StringUtil.simpleClassName(group))
                    .append(", ");
        }
        @SuppressWarnings("deprecation")
        ChannelFactory<? extends C> factory = channelFactory();
        if (factory != null) {
            buf.append("channelFactory: ")
                    .append(factory)
                    .append(", ");
        }
        SocketAddress localAddress = localAddress();
        if (localAddress != null) {
            buf.append("localAddress: ")
                    .append(localAddress)
                    .append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }


}
