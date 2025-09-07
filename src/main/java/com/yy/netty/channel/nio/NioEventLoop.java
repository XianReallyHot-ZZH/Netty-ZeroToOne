package com.yy.netty.channel.nio;

import com.yy.netty.channel.EventLoopTaskQueueFactory;
import com.yy.netty.channel.SingleThreadEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @Description:NIO类型事件循环器（执行器），nio中selector各种事件，都由该类处理，对ServerSocketChannel和SocketChannel现在还是耦合在一起实现的，这块后续再改造
 */
public class NioEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    private final ServerSocketChannel serverSocketChannel;

    private final SocketChannel socketChannel;

    // 多路复用选择器，一个NIO事件循环器对应一个多路复用选择器
    private final Selector selector;

    // 多路复用选择器的提供者
    private final SelectorProvider selectorProvider;

    // 对应服务端的IO读eventLoop，这个这样写不好，但是先就这样处理了
    private NioEventLoop worker;

    /**
     * @param serverSocketChannel
     * @param socketChannel
     * @Description:构造方法,这一版其实耦合的是比较重的，ServerSocketChannel和SocketChannel是需要使用时进行择一输入的，以实现区分创建的是服务端还是客户端的EventLoop，后面会改，这里先这样
     */
    public NioEventLoop(ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {
        this(null, SelectorProvider.provider(), null, serverSocketChannel, socketChannel);
    }

    /**
     * 构造方法
     *
     * @param executor            创建线程的执行器,该单线程执行器中的线程就是由这个执行器创建而来
     * @param selectorProvider    selector的提供者
     * @param queueFactory        任务队列工厂，创建一个任务队列，该任务队列中存放的是需要执行的任务
     * @param serverSocketChannel 服务端SocketChannel
     * @param socketChannel       客户端SocketChannel
     */
    public NioEventLoop(Executor executor, SelectorProvider selectorProvider, EventLoopTaskQueueFactory queueFactory,
                        ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {
        super(executor, queueFactory);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (serverSocketChannel == null && socketChannel == null) {
            throw new NullPointerException("serverSocketChannel and socketChannel can not be both null");
        }
        if (serverSocketChannel != null && socketChannel != null) {
            throw new IllegalArgumentException("serverSocketChannel and socketChannel can not be both set, only one channel can be here! server or client!");
        }
        this.selectorProvider = selectorProvider;
        this.serverSocketChannel = serverSocketChannel;
        this.socketChannel = socketChannel;
        this.selector = openSelector();
    }


    public void setWorker(NioEventLoop worker) {
        this.worker = worker;
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
     * <核心方法>
     * 在这里定义NioEventLoop的循环逻辑
     * </核心方法>
     */
    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    protected void run() {
        for (;;) {
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
            iterator.remove();
            //处理就绪事件
            processSelectedKey(key);
        } while (iterator.hasNext());
    }

    /**
     * <比较关键的方法>
     * 根据SelectionKey处理单条IO事件
     * 由于这里我们并不知道当前的这个NioEventLoop是服务端还是客户端，所以这里代码实现上会比较耦合，需要判断根据serverSocketChannel和socketChannel的值来判断，两者只会存在其一
     * 这里的代码是比较丑陋了，后续重构，会仿写netty，把这部分优化掉
     * </比较关键的方法>
     *
     * @param key
     */
    private void processSelectedKey(SelectionKey key) throws IOException {
        //说明传进来的是客户端channel，要处理客户端的事件
        if (socketChannel != null) {
            // 如果是连接事件
            if (key.isConnectable()) {
                logger.info("客户端处理IO连接事件");
                //channel已经连接成功
                if (socketChannel.finishConnect()) {
                    //注册读事件
//                    socketChannel.register(selector, SelectionKey.OP_READ);
                    // 客户端也可以像服务端一样将IO读事件的处理用worker事件循环器处理
                    worker.registerRead(socketChannel, worker);
                }
            }
            //如果是读事件
            if (key.isReadable()) {
                logger.info("客户端处理IO读事件");
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int len = socketChannel.read(byteBuffer);
                byte[] buffer = new byte[len];
                byteBuffer.flip();
                byteBuffer.get(buffer);
                logger.info("客户端收到消息：{}", new String(buffer));
//                // 临时测试一下，下面代码记得删掉
//                socketChannel.write(ByteBuffer.wrap("我是客户端，我接收到了服务端连接确认的信息".getBytes()));
            }
            return;
        }
        //运行到这里说明是服务端的channel
        if (serverSocketChannel != null) {
            //连接事件
            if (key.isAcceptable()) {
                logger.info("服务端处理IO连接事件");
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                // 交给worker事件循环器处理，服务端的主eventLoop只处理连接事件，worker处理读事件
                worker.registerRead(socketChannel, worker);
                socketChannel.write(ByteBuffer.wrap("我还不是netty，但我知道你上线了".getBytes()));
                logger.info("服务器接收到客户端连接后，向客户端发送消息成功！");
            }
            //读事件
            if (key.isReadable()) {
                logger.info("服务端处理IO读事件");
                SocketChannel channel = (SocketChannel)key.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int len = channel.read(byteBuffer);
                if (len == -1) {
                    logger.info("客户端通道要关闭！");
                    channel.close();
                    return;
                }
                byte[] buffer = new byte[len];
                byteBuffer.flip();
                byteBuffer.get(buffer);
                logger.info("服务端收到客户端发送的数据:{}",new String(buffer));
            }
        }
    }

    private void select() throws IOException {
        Selector selector = this.selector;
        //这里是一个死循环, 直到有IO事件到来或者任务队列中有任务, 不然就需要用循环来实现阻塞的效果
        for (;;) {
            //如果没有就绪事件，就在这里阻塞3秒
            int selectedKeyNum = selector.select(3000);
            //如果有事件或者单线程执行器中有任务待执行，就退出循环
            if (selectedKeyNum != 0 || hasTasks()) {
                break;
            }
        }
    }


    public Selector unwrappedSelector() {
        return selector;
    }
}
