package com.yy.netty.bootstrap;

import com.yy.netty.channel.Channel;
import com.yy.netty.channel.ChannelOption;
import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description:服务端Socket网络搭建引导类，引导实现对ServerSocketChannel的NIO事件处理，最终实现服务端ip：port的绑定，同时接受各个客户端的连接请求和数据的io读取
 */
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, Channel> {

    private static final Logger logger = LoggerFactory.getLogger(ServerBootstrap.class);

    // 服务端 work事件循环组, 负责处理IO read/write事件（读写事件）
    private EventLoopGroup childGroup;

    // 服务端 child事件循环组, 负责处理IO read/write事件（读写事件）
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();

    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);


    public ServerBootstrap() {

    }

    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
    }

    /**
     * 一般不会用这个方法
     *
     * @param group
     * @return
     */
    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * 设置boss和work线程组,一般是用这个的哈
     *
     * @param parentGroup
     * @param childGroup
     * @return
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        ObjectUtil.checkNotNull(childGroup, "childGroup");
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }

    /**
     * 设置childGroup的参数
     *
     * @param childOption
     * @param value
     * @param <T>
     * @return
     */
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        ObjectUtil.checkNotNull(childOption, "childOption");
        if (value == null) {
            synchronized (childOptions) {
                childOptions.remove(childOption);
            }
        } else {
            synchronized (childOptions) {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    /**
     * 初始化channel
     * 主要是增加服务端channel的参数的设置逻辑
     *
     * @param channel
     * @throws Exception
     */
    @Override
    void init(Channel channel) throws Exception {
        // 得到父类中存储的所有参数项及其值
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            // 把初始化时用户配置的参数全都放到channel的config类中
            setChannelOptions(channel, options);
        }
    }

    /**
     * 重写父类validate方法，增加对childGroup的判断，因为服务端的work线程组是必须的，需要被生成的客户端channel注册
     *
     * @return
     */
    @Override
    public ServerBootstrap validate() {
        super.validate();
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    public EventLoopGroup childGroup() {
        return childGroup;
    }

    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }

    private static Map.Entry<ChannelOption<?>, Object>[] newOptionArray(int size) {
        return new Map.Entry[size];
    }


}
