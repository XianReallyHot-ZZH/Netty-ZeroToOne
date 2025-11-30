package com.yy.netty.util;

import com.yy.netty.util.internal.ObjectUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 常量池，其实是个ConcurrentMap，以key-value的方式存储用户定义的配置项信息
 * @param <T>
 */
public abstract class ConstantPool<T extends Constant<T>> {

    // 存储常量的池子
    private final ConcurrentMap<String, T> constants = new ConcurrentHashMap<>();

    // 用来初始化赋值给常量类的id，初值为1
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * 根据常量的name获取对应的常量对象
     *
     * @param name
     * @return
     */
    public T valueOf(String name) {
        checkNotNullAndNotEmpty(name);
        return getOrCreate(name);
    }

    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        if (firstNameComponent == null) {
            throw new NullPointerException("firstNameComponent");
        }
        if (secondNameComponent == null) {
            throw new NullPointerException("secondNameComponent");
        }
        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    /**
     * 判断常量是否存在，也就是常量池中是否有常量的key
     *
     * @param name
     * @return
     */
    public boolean exists(String name) {
        checkNotNullAndNotEmpty(name);
        return constants.containsKey(name);
    }

    /**
     * 根据指定的name直接生成对应的常量对象,如果该常量已经生成在使用了，那么会抛出异常
     *
     * @param name
     * @return
     */
    public T newInstance(String name) {
        checkNotNullAndNotEmpty(name);
        return createOrThrow(name);
    }

    /**
     * 根据常量name，获取或者创建对应的常量对象
     *
     * @param name
     * @return
     */
    private T getOrCreate(String name) {
        // 尝试从池子中获取
        T constant = constants.get(name);
        //先判断常量池中是否有该常量
        if (constant == null) {
            //没有的话就创建
            final T tempConstant = newConstant(nextId(), name);
            //然后放进常量池中
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }
        //最后返回该常量
        return constant;
    }

    /**
     * 该方法和getOrCreate并没什么区别，只不过该方法会在创建失败后抛出异常，表示该常量已经在使用了
     *
     * @param name
     * @return
     */
    private T createOrThrow(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = newConstant(nextId(), name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }
        throw new IllegalArgumentException(String.format("'%s' is already in use", name));
    }



    private static String checkNotNullAndNotEmpty(String name) {
        ObjectUtil.checkNotNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
        return name;
    }

    /**
     * 由具体的子类来实现生成具体常量的具体逻辑
     *
     * @param id
     * @param name
     * @return
     */
    protected abstract T newConstant(int id, String name);

    public final int nextId() {
        return nextId.getAndIncrement();
    }

}
