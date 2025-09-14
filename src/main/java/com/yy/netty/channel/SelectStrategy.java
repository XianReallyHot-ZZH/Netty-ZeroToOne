package com.yy.netty.channel;

import java.util.function.IntSupplier;

/**
 * 选择策略
 */
public interface SelectStrategy {

    int SELECT = -1;

    int CONTINUE = -2;

    int BUSY_WAIT = -3;

    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;

}
