package com.yy.netty.util;

/**
 * 该类也是一个常量类，该常量最终会用在AttributeMap中，AttributeMap中存储的key就是AttributeKey
 * 该常量和ChannelOption类似，定位为常量，但是这里用于描述的是属性，常量的值是不关心的，其实实现里也是没有的。
 *
 * @param <T>
 */
public final class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {



    private AttributeKey(int id, String name) {
        super(id, name);
    }
}
