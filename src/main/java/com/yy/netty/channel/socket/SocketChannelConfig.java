package com.yy.netty.channel.socket;

import com.yy.netty.channel.ChannelConfig;

/**
 * 客户端SocketChannel的配置类，在ChannelConfig的基础上增加了若干服务端SocketChannel的配置项
 */
public interface SocketChannelConfig extends ChannelConfig {

    boolean isTcpNoDelay();

    SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay);

    int getSoLinger();

    SocketChannelConfig setSoLinger(int soLinger);

    int getSendBufferSize();

    SocketChannelConfig setSendBufferSize(int sendBufferSize);

    int getReceiveBufferSize();

    SocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    boolean isKeepAlive();

    SocketChannelConfig setKeepAlive(boolean keepAlive);

    int getTrafficClass();

    SocketChannelConfig setTrafficClass(int trafficClass);

    boolean isReuseAddress();

    SocketChannelConfig setReuseAddress(boolean reuseAddress);

    SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    boolean isAllowHalfClosure();

    SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure);

    // --------------------------- 这几个是父类里的，这里只是为了重构返回值类型 -------------------------
    @Override
    SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    SocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    SocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    SocketChannelConfig setAutoClose(boolean autoClose);

}
