package com.yy.netty.util.concurrent;

/**
 * @Description:在netty中，返回的future中，基本都是该接口的实现类
 * @param <V>
 */
public interface Promise<V> extends Future<V> {

    /**
     * 为任务设置成功结果，一旦设置成功结果，则任务就结束了，同时会唤醒等待该任务的线程，如果失败，那么会抛出异常
     *
     * @param result
     * @return
     */
    Promise<V> setSuccess(V result);

    /**
     * 尝试设置成功结果，如果设置结果成功，则返回true，否则返回false;如果设置成功，则任务就结束了，同时会唤醒等待该任务的线程
     *
     * @param result
     * @return
     */
    boolean trySuccess(V result);

    /**
     * 为任务设置异常结果，如果设置成功，则任务就结束了，同时会唤醒等待该任务的线程，如果失败，那么会抛出异常
     *
     * @param cause
     * @return
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * 尝试设置异常结果，如果设置结果成功，则返回true，否则返回false;如果设置成功，则任务就结束了，同时会唤醒等待该任务的线程
     *
     * @param cause
     * @return
     */
    boolean tryFailure(Throwable cause);

    /**
     * 设置当前的任务为不可取消
     *
     * @return
     */
    boolean setUncancellable();

    /******************************************** 一下四个方法是为了 重塑返回对象为Promise ********************************************/
    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
