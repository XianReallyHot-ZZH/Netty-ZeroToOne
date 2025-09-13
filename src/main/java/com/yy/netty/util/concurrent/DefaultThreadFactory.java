package com.yy.netty.util.concurrent;

import com.yy.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:默认线程工厂类
 */
public class DefaultThreadFactory implements ThreadFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultThreadFactory.class);

    private static final AtomicInteger POOL_ID = new AtomicInteger();

    // 线程名称前缀
    private final String prefix;
    // id生成器
    private final AtomicInteger nextId = new AtomicInteger();
    // 线程是否是守护线程
    private final boolean daemon;
    // 线程优先级
    private final int priority;
    // 线程组
    protected final ThreadGroup threadGroup;


    public DefaultThreadFactory() {
        this(DefaultThreadFactory.class);
    }

    public DefaultThreadFactory(Class<?> poolType) {
        //设置为非守护线程，优先级为5
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(toPoolName(poolType), daemon, priority);
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, Thread.currentThread().getThreadGroup());
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (poolName == null) {
            throw new NullPointerException("poolName");
        }
        //校验线程优先级
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }
        //给属性赋值
        prefix = poolName + '-' + POOL_ID.incrementAndGet() + '-';
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    public static String toPoolName(Class<?> poolType) {
        if (poolType == null) {
            throw new NullPointerException("poolType");
        }

        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                return "unknown";
            case 1:
                return poolName.toLowerCase(Locale.US);
            default:
                if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {
                    return poolName;
                }
        }

    }


    /**
     * <核心方法>
     *     为任务创建线程
     * </核心方法>
     *
     * @param r a runnable to be executed by new thread instance
     * @return
     */
    @Override
    public Thread newThread(Runnable r) {
        // 新建线程
        Thread thread = new Thread(r, prefix + nextId.incrementAndGet());
        logger.info("新建线程：{}", thread.getName());
        try {
            if (thread.isDaemon() != daemon) {
                thread.setDaemon(daemon);
            }
            if (thread.getPriority() != priority) {
                thread.setPriority(priority);
            }
        } catch (Exception e) {
            logger.error("创建线程失败:{}",e.getMessage());
        }

        return thread;
    }
}
