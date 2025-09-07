package com.yy.netty.util.concurrent;

import com.yy.netty.channel.EventLoopTaskQueueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @Description:单线程执行器，实际上这个类就是一个单线程的线程池，netty中所有任务都是被该执行器执行的 既然是执行器(虽然该执行器中只有一个无限循环的线程工作)，但执行器应该具备的属性也不可少，比如任务队列，拒绝策略等等
 */
public abstract class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    //执行器的初始状态，未启动
    private static final int ST_NOT_STARTED = 1;

    //执行器启动后的状态
    private static final int ST_STARTED = 2;

    // 执行器的状态
    private volatile int state = ST_NOT_STARTED;

    // 执行器的状态更新器,也是一个原子类，通过cas来改变执行器的状态值
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");

    // 任务队列的容量，默认是Integer的最大值
    private static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;

    // 任务队列
    private final Queue<Runnable> taskQueue;

    // 拒绝策略
    private final RejectedExecutionHandler rejectedExecutionHandler;

    // 真正在干活的那个执行线程
    private volatile Thread thread;

    //创建线程的执行器,该单线程执行器中的线程就是由这个执行器创建而来
    private Executor executor;

    // 线程是否被中断的信号，在线程执行逻辑中会有地方使用该变量进行判断，以达到中断线程的效果
    private volatile boolean interrupted;


    /**
     * 创建单线程执行器
     *
     * @param executor  创建线程的执行器,该单线程执行器中的线程就是由这个执行器创建而来
     * @param queueFactory  任务队列工厂，该工厂会创建任务队列
     * @param threadFactory 线程工厂，负责创建线程
     */
    protected SingleThreadEventExecutor(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory) {
        this(executor, queueFactory, threadFactory, RejectedExecutionHandlers.reject());
    }

    protected SingleThreadEventExecutor(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        if (executor == null) {
            this.executor = new ThreadPerTaskExecutor(threadFactory);
        }
        this.taskQueue = queueFactory == null ? newTaskQueue(DEFAULT_MAX_PENDING_TASKS) : queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
    }


    /**
     * <核心方法>
     * 单线程执行器的核心循环逻辑入口，交给子类实现，当单线程被创建后，在线程中会吊起该方法
     * </核心方法>
     */
    protected abstract void run();

    /**
     * <核心方法>
     * 往执行器提交任务，如果当前的单线程执行器中的线程不存在，就创建一个线程并启动
     * </核心方法>
     *
     * @param task the runnable task
     */
    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        //把任务提交到任务队列中
        addTask(task);
        //启动单线程执行器中的线程
        startThread();
    }

    private void startThread() {
        //暂时先不考虑特别全面的线程池状态，只关心线程是否已经启动
        //如果执行器的状态是未启动，就cas将其状态值变为已启动
        if (state == ST_NOT_STARTED && STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
            boolean success = false;
            try {
                doStartThread();
                success = true;
            } finally {
                //如果启动未成功，直接把状态值复原
                if (!success) {
                    STATE_UPDATER.compareAndSet(this, ST_STARTED, ST_NOT_STARTED);
                }
            }
        }
    }

    private void doStartThread() {
        //这里的executor是ThreadPerTaskExecutor，会为传入的每一个任务创建一个线程
        //在传入的任务中会调用nioeventloop中的run方法，无限循环，直到资源被释放
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //Thread.currentThread得到的就是正在执行任务的单线程执行器的线程，这里把它赋值给thread属性
                thread = Thread.currentThread();
                if (interrupted) {
                    thread.interrupt();
                }
                //线程开始轮询处理IO事件，父类中的关键字this代表的是子类对象，这里调用的是nioeventloop中的run方法
                SingleThreadEventExecutor.this.run();
                logger.info("单线程执行器的线程退出，错误结束了！");
            }
        });


    }

    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        //尝试添加任务至任务队列中，如果添加失败，执行拒绝策略
        if (!offerTask(task)) {
            reject(task);
        }
    }

    private void reject(Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    private boolean offerTask(Runnable task) {
        return taskQueue.offer(task);
    }

    /**
     * @Description:判断传入的线程是否是当前执行器的线程
     *
     * @param thread
     * @return
     */
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    /**
     * @Description:判断任务队列中是否有任务
     *
     * @return
     */
    protected boolean hasTasks() {
        if (taskQueue.isEmpty()) {
            logger.debug("我没任务了！");
            return false;
        }
        logger.info("有任务了！");
        return true;
    }

    /**
     * 取出当前队列中的所有任务并执行
     */
    protected void runAllTasks() {
        runAllTasksFrom(taskQueue);
    }

    protected void runAllTasksFrom(Queue<Runnable> taskQueue) {
        //从任务对立中拉取任务,如果第一次拉取就为null，说明任务队列中没有任务，直接返回即可
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }

        for (;;) {
            //执行任务队列中的任务
            safeExecute(task);
            //执行完毕之后，拉取下一个任务，如果为null就直接返回
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }

    protected void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    protected Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        return taskQueue.poll();
    }


    /**
     * @Description: 中断单线程执行器中的线程
     */
    protected void interruptThread() {
        Thread currentThread = this.thread;
        if (currentThread != null) {
            // 将中断标记进行记录，在启动单线程的时候会判断该标记，如果标记为true，则调用interrupt方法，以保持不同时序时中断的效果
            interrupted = true;
        } else {
            //中断线程并不是直接让该线程停止运行，而是提供一个中断信号
            //也就是标记，想要停止线程仍需要在运行流程中结合中断标记来判断
            currentThread.interrupt();
        }
    }


}
