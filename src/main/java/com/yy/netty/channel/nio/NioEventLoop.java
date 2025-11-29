package com.yy.netty.channel.nio;

import com.yy.netty.channel.*;
import com.yy.netty.util.concurrent.RejectedExecutionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Description:NIO类型事件循环器（执行器），nio中selector各种事件，包括netty的处理事件，都由该类处理
 */
public class NioEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    private static int index = 0;

    // id
    private final int id;

    // 多路复用选择器，一个NIO事件循环器对应一个多路复用选择器
    private final Selector selector;

    // 多路复用选择器的提供者
    private final SelectorProvider selectorProvider;

    // 选择策略
    private SelectStrategy selectStrategy;


    /**
     * 构造方法
     *
     * @param parent                   当前单线程事件循环器所属的事件循环器组
     * @param executor                 创建线程的执行器,该单线程执行器中的线程就是由这个执行器创建而来
     * @param selectorProvider         selector的提供者
     * @param strategy                 选择策略
     * @param rejectedExecutionHandler 拒绝策略
     * @param queueFactory             任务队列工厂
     */
    public NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                        SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                        EventLoopTaskQueueFactory queueFactory) {
        super(parent, executor, false, newTaskQueue(queueFactory), rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        this.selectorProvider = selectorProvider;
        this.selector = openSelector();
        this.selectStrategy = strategy;
        id = ++index;
        logger.info("我是第{}个nioEventLoop, id:{}", index, id);
    }

    private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory) {
        if (queueFactory == null) {
            return new LinkedBlockingQueue<>(DEFAULT_MAX_PENDING_TASKS);
        }
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }

    // 得到该EventLoop的用于轮询的选择器
    private Selector openSelector() {
        //未包装过的选择器
        final Selector unwrappedSelector;
        try {
            unwrappedSelector = selectorProvider.openSelector();
            return unwrappedSelector;
        } catch (Exception e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }

    /**
     * 获取该EventLoop唯一绑定的Selector
     *
     * @return
     */
    public Selector unwrappedSelector() {
        return selector;
    }

    /**
     * <核心方法>
     * 在这里定义NioEventLoop的循环逻辑
     * </核心方法>
     */
    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    protected void run() {
        for (; ; ) {
            try {
                //没有事件就阻塞在这里,跳出select阻塞的条件是：有IO事件到来 或者 任务队列中有任务
                select();
                // 如果有事件,就处理就绪事件
                processSelectedKeys();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }

    private void select() throws IOException {
        Selector selector = this.selector;
        //这里是一个死循环, 直到有IO事件到来或者任务队列中有任务, 不然就需要用循环来实现阻塞的效果
        for (; ; ) {
            //如果没有就绪事件，就在这里阻塞3秒
            int selectedKeyNum = selector.select(3000);
            //如果有事件或者单线程执行器中有任务待执行，就退出循环；否则就继续循环
            if (selectedKeyNum != 0 || hasTasks()) {
                break;
            }
        }
    }


    private void processSelectedKeys() throws Exception {
        processSelectedKeysPlain(selector.selectedKeys());
    }

    /**
     * 完成对事件的响应处理
     *
     * @param selectedKeys
     * @throws Exception
     */
    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) throws Exception {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        do {
            SelectionKey key = iterator.next();
            //还记得channel在注册时的第三个参数this吗？这里通过attachment方法就可以得到nio类的channel
            final Object nettyChannel = key.attachment();
            iterator.remove();
            //处理就绪事件
            if (nettyChannel instanceof AbstractNioChannel) {
                // 其实这里key和channel是一对，channel中会持有该key
                processSelectedKey(key, (AbstractNioChannel) nettyChannel);
            }
        } while (iterator.hasNext());
    }

    /**
     * <比较关键的方法>
     * 根据SelectionKey处理单条IO事件,这里借助Channel体系，将IO事件委托给相应的channel来处理，实现了解耦
     * 其实这里入参key和channel是一对，channel中会持有该key，IO的具体处理可以委托给相应的channel来完成
     * </比较关键的方法>
     *
     * @param key
     * @param ch
     */
    private void processSelectedKey(SelectionKey key, AbstractNioChannel ch) throws Exception {
        try {
            //获取Unsafe类
            final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
            //得到key感兴趣的事件
            int ops = key.interestOps();
            //如果是连接事件,该事件只会出现在客户端channel中
            if (ops == SelectionKey.OP_CONNECT) {
                //位运算，实现移除连接事件，否则会一直通知，这里实际上是做了个减法
                ops &= ~SelectionKey.OP_CONNECT;
                //刷新感兴趣的事件，其实还是在做清理
                key.interestOps(ops);
                //走到这里说明客户端channel连接成功了，那么就可以真正为该channel注册读事件，开始进入循环处理客户端IO读事件了
                ch.doBeginRead();
                // 这里要做客户端真正的连接处理
                unsafe.finishConnect();
            }

            // 下面两个逻辑，其实就是把具体的read实现委托给了具体的channel，这个具体的channel其实就是key上作为附件绑定的那个具体的netty channel了
            if (ops == SelectionKey.OP_READ) {
                // 其实只有客户端channel才会触发OP_READ事件，很自然的，这里ch的实例肯定就是NioSocketChannel
                unsafe.read();
            }
            if (ops == SelectionKey.OP_ACCEPT) {
                // 服务端channel才会触发OP_ACCEPT事件，很自然的，这里ch的实例肯定就是NioServerSocketChannel
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            throw new RuntimeException(ignored.getMessage());
        }
    }
}
