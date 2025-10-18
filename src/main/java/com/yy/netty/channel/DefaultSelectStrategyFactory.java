package com.yy.netty.channel;

/**
 * @Description:默认的选择策略工厂
 */
public final class DefaultSelectStrategyFactory implements SelectStrategyFactory {

    public static final SelectStrategyFactory INSTANCE = new DefaultSelectStrategyFactory();

    private DefaultSelectStrategyFactory() {
    }

    @Override
    public SelectStrategy newSelectStrategy() {
        return DefaultSelectStrategy.INSTANCE;
    }

}
