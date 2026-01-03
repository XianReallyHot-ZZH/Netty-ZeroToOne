package com.yy.netty.util;

/**
 * AttributeMap的实现类，AbstractChannel后续会继承该实现类，进而赋予channel map容器的角色，那么channel其实就可以当做容器来存储数据了
 */
public class DefaultAttributeMap implements AttributeMap {

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }
}
