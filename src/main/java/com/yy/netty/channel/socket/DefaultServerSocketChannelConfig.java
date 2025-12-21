package com.yy.netty.channel.socket;

import com.yy.netty.channel.ChannelOption;
import com.yy.netty.channel.DefaultChannelConfig;
import com.yy.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;

import static com.yy.netty.channel.ChannelOption.*;
import static com.yy.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * 服务端，ServerSocketChannelConfig的默认实现类，子类NioServerSocketChannel默认使用的配置类
 * 在这一层会完成对服务端Socket的配置+管理
 */
public class DefaultServerSocketChannelConfig extends DefaultChannelConfig implements ServerSocketChannelConfig {

    // jdk的NIO模块的ServerSocket对象
    protected final ServerSocket javaSocket;

    private volatile int backlog = 128;

    public DefaultServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
        super(channel);
        if (javaSocket == null) {
            throw new NullPointerException("javaSocket");
        }
        this.javaSocket = javaSocket;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), SO_RCVBUF, SO_REUSEADDR, SO_BACKLOG);
    }

    /**
     * 重写getOption，支持对服务端Socket的配置项的获取
     *
     * @param option
     * @param <T>
     * @return
     */
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == SO_RCVBUF) {
            return (T) Integer.valueOf(getReceiveBufferSize());
        }
        if (option == SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }
        if (option == SO_BACKLOG) {
            return (T) Integer.valueOf(getBacklog());
        }
        return super.getOption(option);
    }

    /**
     * 重写setOption，支持对服务端Socket的配置项的设置
     *
     * @param option
     * @param value
     * @param <T>
     * @return
     */
    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);
        if (option == SO_RCVBUF) {
            setReceiveBufferSize((Integer) value);
        } else if (option == SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else if (option == SO_BACKLOG) {
            setBacklog((Integer) value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public int getBacklog() {
        return backlog;
    }

    @Override
    public ServerSocketChannelConfig setBacklog(int backlog) {
        checkPositiveOrZero(backlog, "backlog");
        this.backlog = backlog;
        return this;
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return javaSocket.getReuseAddress();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerSocketChannelConfig setReuseAddress(boolean reuseAddress) {
        try {
            javaSocket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return javaSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize) {
        try {
            javaSocket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        javaSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
        return this;
    }

    @Override
    public ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    public ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public ServerSocketChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public ServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public ServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }
}
