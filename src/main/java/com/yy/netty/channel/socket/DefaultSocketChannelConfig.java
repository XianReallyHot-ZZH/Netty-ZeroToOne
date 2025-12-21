package com.yy.netty.channel.socket;

import com.yy.netty.channel.ChannelOption;
import com.yy.netty.channel.DefaultChannelConfig;
import com.yy.netty.channel.socket.nio.NioSocketChannel;

import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

import static com.yy.netty.channel.ChannelOption.*;

/**
 * 客户端，SocketChannelConfig的默认实现类，子类NioSocketChannel默认使用的配置类
 * 在这一层会完成对客户端Socket的配置+管理
 */
public class DefaultSocketChannelConfig extends DefaultChannelConfig implements SocketChannelConfig {

    // jdk的NIO模块的ServerSocket对象
    protected final Socket javaSocket;

    private volatile boolean allowHalfClosure;

    public DefaultSocketChannelConfig(NioSocketChannel channel, Socket javaSocket) {
        super(channel);
        if (javaSocket == null) {
            throw new NullPointerException("javaSocket");
        }
        this.javaSocket = javaSocket;
        setTcpNoDelay(true);
    }

    /**
     * 重写getOptions，支持对客户端Socket的配置项的获取
     *
     * @return
     */
    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), SO_RCVBUF, SO_SNDBUF, TCP_NODELAY, SO_KEEPALIVE, SO_REUSEADDR, SO_LINGER, IP_TOS, ALLOW_HALF_CLOSURE);
    }

    /**
     * 重写getOption，支持对客户端Socket的配置项的获取
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
        if (option == SO_SNDBUF) {
            return (T) Integer.valueOf(getSendBufferSize());
        }
        if (option == TCP_NODELAY) {
            return (T) Boolean.valueOf(isTcpNoDelay());
        }
        if (option == SO_KEEPALIVE) {
            return (T) Boolean.valueOf(isKeepAlive());
        }
        if (option == SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }
        if (option == SO_LINGER) {
            return (T) Integer.valueOf(getSoLinger());
        }
        if (option == IP_TOS) {
            return (T) Integer.valueOf(getTrafficClass());
        }
        if (option == ALLOW_HALF_CLOSURE) {
            return (T) Boolean.valueOf(isAllowHalfClosure());
        }
        return super.getOption(option);
    }

    /**
     * 重写setOption，支持对客户端Socket的配置项的设置
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
        } else if (option == SO_SNDBUF) {
            setSendBufferSize((Integer) value);
        } else if (option == TCP_NODELAY) {
            setTcpNoDelay((Boolean) value);
        } else if (option == SO_KEEPALIVE) {
            setKeepAlive((Boolean) value);
        } else if (option == SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else if (option == SO_LINGER) {
            setSoLinger((Integer) value);
        } else if (option == IP_TOS) {
            setTrafficClass((Integer) value);
        } else if (option == ALLOW_HALF_CLOSURE) {
            setAllowHalfClosure((Boolean) value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    @Override
    public boolean isTcpNoDelay() {
        try {
            return javaSocket.getTcpNoDelay();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay) {
        try {
            javaSocket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public int getSoLinger() {
        try {
            return javaSocket.getSoLinger();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketChannelConfig setSoLinger(int soLinger) {
        try {
            if (soLinger < 0) {
                javaSocket.setSoLinger(false, 0);
            } else {
                javaSocket.setSoLinger(true, soLinger);
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public int getSendBufferSize() {
        try {
            return javaSocket.getSendBufferSize();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketChannelConfig setSendBufferSize(int sendBufferSize) {
        try {
            javaSocket.setSendBufferSize(sendBufferSize);
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
    public SocketChannelConfig setReceiveBufferSize(int receiveBufferSize) {
        try {
            javaSocket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public boolean isKeepAlive() {
        try {
            return javaSocket.getKeepAlive();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketChannelConfig setKeepAlive(boolean keepAlive) {
        try {
            javaSocket.setKeepAlive(keepAlive);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public int getTrafficClass() {
        try {
            return javaSocket.getTrafficClass();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SocketChannelConfig setTrafficClass(int trafficClass) {
        try {
            javaSocket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
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
    public SocketChannelConfig setReuseAddress(boolean reuseAddress) {
        try {
            javaSocket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        javaSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
        return this;
    }

    @Override
    public boolean isAllowHalfClosure() {
        return allowHalfClosure;
    }

    @Override
    public SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure) {
        this.allowHalfClosure = allowHalfClosure;
        return this;
    }

    @Override
    public SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    public SocketChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public SocketChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public SocketChannelConfig setAutoClose(boolean autoClose) {
        super.setAutoClose(autoClose);
        return this;
    }

    @Override
    public SocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public SocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }
}
