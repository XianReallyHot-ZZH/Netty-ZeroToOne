package com.yy.netty.util;

/**
 * 该类也是一个常量类，该常量最终会用在AttributeMap中，AttributeMap中存储的key就是AttributeKey
 * 该常量和ChannelOption类似，定位为常量，但是这里用于描述的是共享参数，常量的值是不关心的，其实实现里也是没有的。
 *
 * @param <T>
 */
public final class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {

    /**
     * 静态常量池,用于存储共享参数key
     */
    private static final ConstantPool<AttributeKey<Object>> pool = new ConstantPool<AttributeKey<Object>>() {
        @Override
        protected AttributeKey<Object> newConstant(int id, String name) {
            return new AttributeKey<>(id, name);
        }
    };

    /**
     * 创建一个AttributeKey对象,如果该name已经存在，那么会抛出异常
     *
     * @param name
     * @param <T>
     * @return
     */
    public static <T> AttributeKey<T> newInstance(String name) {
        return (AttributeKey<T>) pool.newInstance(name);
    }

    /**
     * 根据名称从常量池中获取对应的AttributeKey对象,没有的话就创建
     *
     * @param name
     * @param <T>
     * @return
     */
    public static <T> AttributeKey<T> valueOf(String name) {
        return (AttributeKey<T>) pool.valueOf(name);
    }

    public static <T> AttributeKey<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (AttributeKey<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    public static boolean exists(String name) {
        return pool.exists(name);
    }

    private AttributeKey(int id, String name) {
        super(id, name);
    }
}
