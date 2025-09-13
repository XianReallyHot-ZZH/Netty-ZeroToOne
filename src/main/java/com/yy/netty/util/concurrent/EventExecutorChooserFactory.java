package com.yy.netty.util.concurrent;

/**
 * @Description:事件执行器选择器工厂接口，那肯定是用来生产事件执行器选择器的咯
 */
public interface EventExecutorChooserFactory {


    /**
     * 新建一个事件执行器选择器
     *
     * @param executors
     * @return
     */
    EventExecutorChooser newChooser(EventExecutor[] executors);


    /**
     * 事件执行器选择器接口
     */
    interface EventExecutorChooser {

        /**
         * 选择出一个事件执行器
         *
         * @return
         */
        EventExecutor next();
    }

}
