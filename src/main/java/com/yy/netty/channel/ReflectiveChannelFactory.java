package com.yy.netty.channel;

import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.StringUtil;

import java.lang.reflect.Constructor;

/**
 * 创建指定类型的channel的工厂，要求指定的channel类型（T）必须是channel的子类
 *
 * @param <T>
 */
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {

    // 待创建的channel类型
    private final Class<? extends T> channelClazz;
    // 待创建的channel的无参构造器
    private final Constructor<? extends T> constructor;

    public ReflectiveChannelFactory(Class<? extends T> channelClazz) {
        ObjectUtil.checkNotNull(channelClazz, "channelClazz");
        this.channelClazz = channelClazz;
        try {
            this.constructor = channelClazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + StringUtil.simpleClassName(channelClazz) + " does not have a public non-arg constructor", e);
        }
    }

    @Override
    public T newChannel() {
        try {
            return constructor.newInstance();
        } catch (Throwable t) {
//            throw new RuntimeException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
            throw new RuntimeException("Unable to create Channel from class " + channelClazz.getName(), t);
        }
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) + "(" + channelClazz.getName() + ".class)";
    }

}
