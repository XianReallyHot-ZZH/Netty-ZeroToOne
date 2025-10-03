package com.yy.netty.util.concurrent;

import java.util.EventListener;

public interface GenericFutureListener<F extends Future<?>> extends EventListener {

    /**
     * 当future完成时，会调用该方法
     *
     * @param future
     */
    void operationComplete(F future);

}
