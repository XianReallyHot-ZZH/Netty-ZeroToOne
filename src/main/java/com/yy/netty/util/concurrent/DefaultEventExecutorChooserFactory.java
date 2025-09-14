package com.yy.netty.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:执行器选择工厂的默认实现类
 */
public final class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {

    // 单例
    public static final EventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();

    private DefaultEventExecutorChooserFactory() {
    }

    @Override
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        //如果数组的长度是2的幂次方就返回PowerOfTwoEventExecutorChooser选择器
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoEventExecutorChooser(executors);
        } else {
            //如果数组长度不是2的幂次方就返回通用选择器。其实看到2的幂次方，应该就可以想到考虑的是位运算，hashmap中是不是也有相同的
            //思想呢？计算数据下标的时候。&会比直接%效率要快一点。这就是netty中微不足道的一个小小优化。
            return new GenericEventExecutorChooser(executors);
        }
    }

    /**
     * 判断是否是2的幂
     *
     * @param val
     * @return
     */
    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    /**
     * 当执行器的数量是2的幂时，使用PowerOfTwoEventExecutorChooser，相当于就是一个轮询器了，位运算效率会高一点
     */
    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {

        private final AtomicInteger idx = new AtomicInteger();
        private final EventExecutor[] executors;

        PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            //idx初始化为0，之后每一次调用next方法，idx都会自增，这样经过运算后，得到的数组下标就会成为一个循环，执行器也就会被循环获取
            //也就是轮询
            return executors[idx.getAndIncrement() & executors.length - 1];
        }

    }

    /**
     * 当执行器的数量不是2的幂时，使用GenericEventExecutorChooser,这个其实也是一个轮询器，但是适用性更广，但是效率会差一点
     */
    private static final class GenericEventExecutorChooser implements EventExecutorChooser {

        private final AtomicInteger idx = new AtomicInteger();
        private final EventExecutor[] executors;

        GenericEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            // idx初始化为0，之后每一次调用next方法，idx都会自增，这样经过运算后，得到的数组下标就会成为一个循环，执行器也就会被循环获取
            // 也是轮询
            return executors[Math.abs(idx.getAndIncrement() % executors.length)];
        }

    }



}
