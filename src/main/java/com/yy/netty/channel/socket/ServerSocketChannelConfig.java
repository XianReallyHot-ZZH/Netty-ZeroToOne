package com.yy.netty.channel.socket;

import com.yy.netty.channel.ChannelConfig;

/**
 * 服务端SocketChannel的配置类,在ChannelConfig的基础上增加了若干服务端SocketChannel的配置项
 */
public interface ServerSocketChannelConfig extends ChannelConfig {

    int getBacklog();

    ServerSocketChannelConfig setBacklog(int backlog);

    boolean isReuseAddress();

    ServerSocketChannelConfig setReuseAddress(boolean reuseAddress);

    int getReceiveBufferSize();

    ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    // --------------------------- 这几个是父类里的，这里只是为了重构返回值类型 -------------------------
    @Override
    ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    ServerSocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    ServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

}
