package com.yy.netty.util.concurrent;

/**
 * @Description:事件执行组的抽象默认实现
 */
public abstract class AbstractEventExecutorGroup implements EventExecutorGroup {

    /**
     * 往执行器组提交任务时，会获取到组内的下一个执行器，然后将任务提交给该执行器执行
     *
     * @param command the runnable task
     */
    @Override
    public void execute(Runnable command) {
        next().execute(command);
    }

}
