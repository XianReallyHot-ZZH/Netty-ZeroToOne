package com.yy.netty.channel.socket.nio;

import com.yy.netty.channel.ChannelOption;

import java.net.SocketOption;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 适配JDK中NIO的原生SocketOption的扩展出来的ChannelOption配置项
 * 这个配置项会更特别一点，针对的是能直接配到JDK原生NetworkChannel的配置项-SocketOption，并且提供了能直接设置到JDK原生NetworkChannel的API（比如设置配置项的值的setOption这种方法）
 * 这类配置项真正起效是在NIO原生体系里的，‘游离’在netty的channelConfig体系之外的，其实应该表述为netty的channelConfig体系是囊括了这部分。
 *
 * @param <T>
 */
public final class NioChannelOption<T> extends ChannelOption<T> {

    /*
     * JDK里NIO的原生SocketOption，netty里自定义的配置项体系是ChannelOption，两者是扩展的关系
     * SocketOption这个配置项由于是NIO原生的，所以最终配置项的值会被直接设置到JDK的NetworkChannel中（使用的NetworkChannel的Option体系）
     */
    private final SocketOption<T> option;

    private NioChannelOption(SocketOption<T> option) {
        // 调用父类构造方法，完成ChannelOption的构建
        super(option.name());
        // 保存SocketOption
        this.option = option;
    }

    /**
     * 创建一个NioChannelOption
     *
     * @param option
     * @param <T>
     * @return
     */
    public static <T> ChannelOption<T> of(SocketOption<T> option) {
        return new NioChannelOption<T>(option);
    }

    /**
     * 往指定的JDK-NetworkChannel设置NioChannelOption配置项的值
     *
     * @param jdkChannel JDK的NetworkChannel
     * @param option     NioChannelOption
     * @param value      配置项的值
     * @param <T>
     * @return
     */
    public static <T> boolean setOption(Channel jdkChannel, NioChannelOption<T> option, T value) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;
        // 判断当前JDK的NetworkChannel是否支持这个配置项
        if (!channel.supportedOptions().contains(option.option)) {
            return false;
        }
        // IP_TOS:用于设置 IP 数据包的"服务类型"字段,它允许应用程序为网络数据包指定服务质量(QoS)参数
        // ServerSocketChannel 作为服务器端监听连接的通道，通常不需要设置这类传输层的服务质量参数
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {
            return false;
        }
        try {
            // 调用JDK的NetworkChannel的setOption方法设置配置项
            channel.setOption(option.option, value);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定的JDK-NetworkChannel的NioChannelOption配置项的值
     *
     * @param jdkChannel
     * @param option
     * @param <T>
     * @return
     */
    public static <T> T getOption(Channel jdkChannel, NioChannelOption<T> option) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;

        if (!channel.supportedOptions().contains(option.option)) {
            return null;
        }
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {
            return null;
        }

        try {
            // 调用JDK的NetworkChannel的getOption方法获取配置项的值
            return channel.getOption(option.option);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定的JDK-NetworkChannel的所有支持的配置项
     *
     * @param jdkChannel
     * @return
     */
    public static ChannelOption[] getOptions(Channel jdkChannel) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;
        Set<SocketOption<?>> supportedOpts = channel.supportedOptions();

        if (channel instanceof ServerSocketChannel) {
            List<ChannelOption<?>> extraOpts = new ArrayList<ChannelOption<?>>(supportedOpts.size());
            // 包装成NioChannelOption，然后返回
            for (SocketOption<?> opt : supportedOpts) {
                // ServerSocketChannel 作为服务器端监听连接的通道，通常不需要设置这类传输层服务质量参数,netty中不支持
                if (opt == java.net.StandardSocketOptions.IP_TOS) {
                    continue;
                }
                extraOpts.add(new NioChannelOption<>(opt));
            }
            return extraOpts.toArray(new ChannelOption[0]);

        } else {
            ChannelOption<?>[] extraOpts = new ChannelOption[supportedOpts.size()];
            // 包装成NioChannelOption，然后返回
            int i = 0;
            for (SocketOption<?> opt : supportedOpts) {
                extraOpts[i++] = new NioChannelOption<>(opt);
            }
            return extraOpts;
        }
    }


}
