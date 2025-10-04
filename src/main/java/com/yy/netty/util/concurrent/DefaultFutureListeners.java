package com.yy.netty.util.concurrent;

import java.util.Arrays;

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

    DefaultFutureListeners(GenericFutureListener<? extends Future<?>> first, GenericFutureListener<? extends Future<?>> second) {
        listeners = new GenericFutureListener[2];
        listeners[0] = first;
        listeners[1] = second;
        size = 2;
        if (first instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
        if (second instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
    }

    /**
     * 获取监听器数组
     *
     * @return
     */
    public GenericFutureListener<? extends Future<?>>[] listeners() {
        return listeners;
    }

    /**
     * 获取可查看进度的监听器数量
     *
     * @return
     */
    public int progressiveSize() {
        return progressiveSize;
    }

    /**
     * 获取监听器数量
     *
     * @return
     */
    public int size() {
        return size;
    }

    /**
     * 添加监听器
     *
     * @param listener
     */
    public void add(GenericFutureListener<? extends Future<?>> listener) {
        GenericFutureListener<? extends Future<?>>[] listeners = this.listeners;
        final int currentSize = this.size;
        // 监听器的数组已满
        if (currentSize == listeners.length) {
            // 数组扩容一倍
            this.listeners = listeners = Arrays.copyOf(listeners, currentSize << 1);
        }
        // 添加监听器
        listeners[currentSize] = listener;
        this.size = currentSize + 1;

        if (listener instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
    }

    /**
     * 移除监听器
     *
     * @param listener
     */
    public void remove(GenericFutureListener<? extends Future<?>> listener) {
        GenericFutureListener<? extends Future<?>>[] listeners = this.listeners;
        int size = this.size;
        for (int i = 0; i < size; i++) {
            // 找到监听器
            if (listeners[i] == listener) {
                // 计算该监听器之后的监听器数量，该数量就是移除该监听器之后需要移动的监听器的数量
                int listenersToMove = size - (i + 1);
                if (listenersToMove > 0) {
                    // 移动监听器
                    System.arraycopy(listeners, i + 1, listeners, i, listenersToMove);
                }
                // 走到这里，说明监听器已经移除了
                listeners[--size] = null;
                this.size = size;

                if (listener instanceof GenericProgressiveFutureListener) {
                    progressiveSize--;
                }
                return;
            }
        }

    }

}
