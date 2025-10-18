package com.yy.netty.channel;

import com.yy.netty.util.IntSupplier;

/**
 * 选择策略
 */
public interface SelectStrategy {

    int SELECT = -1;

    int CONTINUE = -2;

    int BUSY_WAIT = -3;

    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;

}
