package com.yy.netty.channel.nio;

import com.yy.netty.channel.Channel;

import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端NIO消息Channel抽象类，完成一些服务端消息处理的通用方法，持有一些服务端消息处理需要的成员变量
 */
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

    // 当该属性为true时，服务端将不再接受来自客户端的数据
    boolean inputShutdown;

    // 存放服务端建立的客户端连接，先简单处理，该成员变量本来在NioMessageUnsafe静态内部类中
    private final List<Object> readBuf = new ArrayList<Object>();

    /**
     * 构造方法
     *
     * @param parent         服务端产生的客户端SocketChannel才会有父Channel
     * @param ch             对应的NIO Channel
     * @param readInterestOp channel要关注的事件
     */
    protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent, ch, readInterestOp);
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            super.doBeginRead();
        }
        super.doBeginRead();
    }

    /**
     * 子类来具体完成对“read”事件的处理，其实服务端channel这里就是完成对OP_ACCEPT的处理实现了，说白了就是处理连接，生成客户端channel
     *
     * @param buf
     * @return
     */
    protected abstract int doReadMessages(List<Object> buf) throws Exception;

    /**
     * 服务端channel“读”事件的处理逻辑：接受客户端连接，生成客户端channel，把将其注册到工作线程上
     */
    @Override
    protected void read() {
        //该方法要在netty的线程执行器中执行
        assert eventLoop().inEventLoop(Thread.currentThread());
        boolean closed = false;
        Throwable exception = null;
        try {
            do {
                //创建客户端的连接，存放在集合中
                int localRead = doReadMessages(readBuf);
                //返回值为0表示没有连接，直接退出即可
                if (localRead == 0) {
                    break;
                }
            } while (true);
        } catch (Throwable t) {
            exception = t;
        }
        // 处理前面创建的客户端连接
        int size = readBuf.size();
        for (int i = 0; i < size; i++) {
            readPending = false;
            //把每一个客户端的channel注册到工作线程上,这里得不到workgroup，所以我们不在这里实现了，打印一下即可
            Channel child = (Channel) readBuf.get(i);
            System.out.println(child + "收到客户端的channel了");
            // TODO：客户端的channel注册到工作线程上

        }
        //清除集合
        readBuf.clear();
        if (exception != null) {
            throw new RuntimeException(exception);
        }
    }
}
