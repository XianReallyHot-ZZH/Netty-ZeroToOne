package com.yy.netty.util.concurrent;

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
        return null;
    }



}
