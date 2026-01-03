package com.yy.netty.util;

public interface AttributeMap {

    /**
     * 该方法的作用是获取AttributeKey对应的Attribute对象，如果存在，那么直接返回，否则创建一个Attribute对象，并返回
     *
     * @param key
     * @return
     * @param <T>
     */
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * 判断AttributeMap中是否存在对应的AttributeKey
     *
     * @param key
     * @return
     * @param <T>
     */
    <T> boolean hasAttr(AttributeKey<T> key);

}
