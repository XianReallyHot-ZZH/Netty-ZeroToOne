package com.yy.netty.channel;

import com.yy.netty.util.IntSupplier;

public final class DefaultSelectStrategy implements SelectStrategy {

    // 单例
    public static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() {
    }

    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
    }
}
