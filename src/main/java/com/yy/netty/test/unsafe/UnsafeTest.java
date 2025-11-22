package com.yy.netty.test.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unsafe类的使用：尝试获取一个Unsafe实例
 */
public class UnsafeTest {

    private final static Unsafe unsafe = getUnsafe();

    private static Unsafe getUnsafe() {
        Unsafe unsafe = null;
        // 非JDK代码调用Unsafe.getUnsafe()方法会抛出SecurityException异常。所以我们这里得尝试用反射来获取Unsafe实例，Unsafe是Unsafe类中一个静态final成员变量
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            // 获取静态成员变量theUnsafe
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return unsafe;
    }

    public static void main(String[] args) throws NoSuchFieldException {
        System.out.println("unsafe实例获取成功：" + unsafe);

        // 可以自己看一下AtomicInteger这个类的一些常用方法
        AtomicInteger atomicInteger = new AtomicInteger(0);
        System.out.println("AtomicInteger类中的value字段的偏移量：" + unsafe.objectFieldOffset(AtomicInteger.class.getDeclaredField("value")));
        // compareAndSet方法点进去就是unsafe.compareAndSwapInt(this, valueOffset, expect, update)这段源码了，其实这种原子类最终都是依赖unsafe来进行内存操作了
        // 那么这时候我们再看unsafe的其他方法，其实我们就能发现unsafe的定位就是java提供的内存操作集了，提供了很多直接操作对象内存的API
        boolean b = atomicInteger.compareAndSet(0, 1);
        System.out.println("AtomicInteger类中的value字段的CAS操作结果：" + b);

        // (以下来自AI对Unsafe提供的内存操作的分类整理)
        // `Unsafe` 类提供了丰富的底层内存操作API，以下是主要的分类整理：
        //
        //## 内存分配与释放
        //
        //- `allocateMemory(long bytes)` - 分配堆外内存
        //- `reallocateMemory(long address, long bytes)` - 重新分配内存
        //- `freeMemory(long address)` - 释放内存
        //- `setMemory(long address, long bytes, byte value)` - 设置内存块内容
        //
        //## 内存读写操作
        //
        //### 基础类型写入
        //- `putByte(long address, byte x)`
        //- `putShort(long address, short x)`
        //- `putChar(long address, char x)`
        //- `putInt(long address, int x)`
        //- `putLong(long address, long x)`
        //- `putFloat(long address, float x)`
        //- `putDouble(long address, double x)`
        //- `putAddress(long address, long x)`
        //
        //### 基础类型读取
        //- `getByte(long address)`
        //- `getShort(long address)`
        //- `getChar(long address)`
        //- `getLong(long address)`
        //- `getFloat(long address)`
        //- `getDouble(long address)`
        //- `getAddress(long address)`
        //
        //## 对象内存操作
        //
        //### 字段偏移量获取
        //- `objectFieldOffset(Field field)` - 获取对象字段的偏移量
        //- `staticFieldOffset(Field field)` - 获取静态字段的偏移量
        //
        //### 对象字段读写
        //- `getObject(Object o, long offset)` - 读取对象字段
        //- `putObject(Object o, long offset, Object x)` - 写入对象字段
        //- `putInt(Object o, long offset, int x)` - 写入int字段
        //- 其他基础类型的类似方法...
        //
        //## 内存屏障操作
        //
        //- `loadFence()` - 加载屏障
        //- `storeFence()` - 存储屏障
        //- `fullFence()` - 全屏障
        //
        //## 数组操作
        //
        //- `arrayBaseOffset(Class arrayClass)` - 获取数组基础偏移量
        //- `arrayIndexScale(Class arrayClass)` - 获取数组索引缩放因子
        //
        //这些API提供了对内存的底层控制能力，但使用时需要非常谨慎，因为不当使用可能导致内存泄漏或JVM崩溃。
    }

}
