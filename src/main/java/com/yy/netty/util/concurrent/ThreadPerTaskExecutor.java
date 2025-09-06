package com.yy.netty.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @Description:线程创建执行器，netty的执行器中运行的线程都是由这个执行器创建的
 */
public class ThreadPerTaskExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPerTaskExecutor.class);

    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    /**
     * <核心方法>
     *     为传入的任务创建一个线程并启动线程执行任务
     * </核心方法>
     *
     * @param task
     */
    @Override
    public void execute(Runnable task) {
        //在这里创建线程并启动
        threadFactory.newThread(task).start();
        logger.info("真正执行任务的线程被创建了！");
    }


}
