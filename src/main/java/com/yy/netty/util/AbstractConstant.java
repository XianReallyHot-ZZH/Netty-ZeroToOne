package com.yy.netty.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象常量类,常量类的一些通用成员变量、常用方法及其实现
 * @param <T>
 */
public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    // 唯一id生成器
    private static final AtomicLong uniqueIdGenerator = new AtomicLong();

    // 常量 id
    private final int id;

    // 常量名称
    private final String name;

    // uniqueIdGenerator生成的id，这个long类型的id是用来比较常量大小的（同一类常量实现类的大小比较），会在Comparable接口的compareTo方法中被使用
    private final long uniquifier;

    // 构造方法
    protected AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
        this.uniquifier = uniqueIdGenerator.incrementAndGet();
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int compareTo(T o) {
        if (this == o) {
            return 0;
        }

        AbstractConstant<T> other = o;
        int returnCode;

        returnCode = hashCode() - other.hashCode();
        if (returnCode != 0) {
            return returnCode;
        }

        if (uniquifier < other.uniquifier) {
            return -1;
        }
        if (uniquifier > other.uniquifier) {
            return 1;
        }

        throw new Error("failed to compare two different constants");
    }
}
