package com.yy.netty.util.concurrent;

/**
 * @Description:监听器的默认实现类，实际上该类只是对监听器进行了一层包装，内部持有一个监听器的数组，向promise添加的监听器最终都添加到该类的数组中了
 */
public class DefaultFutureListeners {

    // 监听器数组
    private GenericFutureListener<? extends Future<?>>[] listeners;
    // 监听器数量
    private int size;
    // progressive类型监听器的数量
    private int progressiveSize;

}
