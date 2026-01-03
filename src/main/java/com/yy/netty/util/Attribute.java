package com.yy.netty.util;

/**
 * AttributeMap的桶实现中每一个桶的数据结构是一个链表，链表中的每一个节点就是这里的一个Attribute对象
 * 该对象存储了属性项的key和value，其实可以理解为一个entry或者pair对。
 */
public interface Attribute<T> {

    // 属性项的key
    AttributeKey<T> key();

    // ---------------------- 下面是属性项的value操作方法 --------------------------
    T get();

    void set(T value);

    T getAndSet(T value);

    T setIfAbsent(T value);

    boolean compareAndSet(T oldValue, T newValue);

    // ---------------------- 整个属性项的删除方法 --------------------------
    @Deprecated
    T getAndRemove();

    @Deprecated
    void remove();

}
