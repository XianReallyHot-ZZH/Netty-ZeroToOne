package com.yy.netty.channel;

import java.util.Queue;

/**
 * @Description:创建任务队列的工厂
 */
public interface EventLoopTaskQueueFactory {

    Queue<Runnable> newTaskQueue(int maxCapacity);

}
