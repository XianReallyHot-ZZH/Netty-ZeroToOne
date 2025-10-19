package com.yy.netty.channel;

/**
 * channel工厂，创建指定类型的channel
 * @param <T>
 */
public interface ChannelFactory<T extends Channel>{

    T newChannel();

}
