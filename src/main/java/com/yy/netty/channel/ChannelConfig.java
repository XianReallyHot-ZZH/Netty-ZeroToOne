package com.yy.netty.channel;

import java.util.Map;

/**
 * channel的配置接口，赋予channel对配置项的管理，存储，设置起效等功能，通常一个channel会持有一个属于自己的config对象，channel和config是一对一的关系
 * 暂时不定义方法，放到后面再真正引入了
 */
public interface ChannelConfig {

    // 获取channel的所有配置项及其值
    Map<ChannelOption<?>, Object> getOptions();

    // 批量设置channel的配置项及其值
    boolean setOptions(Map<ChannelOption<?>, ?> options);

    // 获取指定channel的配置项的值
    <T> T getOption(ChannelOption<T> option);

    // 设置指定channel的配置项及其值
    <T> boolean setOption(ChannelOption<T> option, T value);

    // ------------------------------------------------------ 一些channel常见、具体的配置项的常见方法 ------------------------------------------------------
    int getConnectTimeoutMillis();

    ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    int getWriteSpinCount();

    ChannelConfig setWriteSpinCount(int writeSpinCount);

    boolean isAutoRead();

    ChannelConfig setAutoRead(boolean autoRead);

    boolean isAutoClose();

    ChannelConfig setAutoClose(boolean autoClose);

    int getWriteBufferHighWaterMark();

    ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    int getWriteBufferLowWaterMark();

    ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

}
