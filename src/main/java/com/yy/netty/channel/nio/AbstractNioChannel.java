package com.yy.netty.channel.nio;

import com.yy.netty.channel.AbstractChannel;
import com.yy.netty.channel.Channel;
import com.yy.netty.channel.ChannelPromise;
import com.yy.netty.channel.EventLoop;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * nio层抽象channel, 在NIO Channel层次中，进行一些技术方法实现，持有一些该层次下的成员变量
 * 该层说白了，就是结合NIO的一些类进行行为封装，最终融合进netty的channel体系中，只是这一层是NIO相关抽象封装的第一层，还会有子类进行更具体的使用封装
 */
public abstract class AbstractNioChannel extends AbstractChannel {

    //该抽象类是NIO中serversocketchannel和socketchannel的公共父类
    private final SelectableChannel ch;

    // 具体channel要关注的事件
    // 比如socketchannel，关注读事件，ServerSocketChannel，关注连接事件
    protected final int readInterestOp;

    //channel注册到selector后返回的key，该key上未来就会出现感兴趣的事件，事件最终会在EventLoop被响应处理
    volatile SelectionKey selectionKey;

    //是否还有未读取的数据
    boolean readPending;

    /**
     * 构造方法
     *
     * @param parent         服务端产生的客户端SocketChannel才会有父Channel
     * @param ch             对应的NIO Channel
     * @param readInterestOp channel要关注的事件
     */
    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;

        try {
            //设置服务端channel为非阻塞模式
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                //有异常直接关闭channel
                ch.close();
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
            throw new RuntimeException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    // 返回NIO原生Channel
    protected SelectableChannel javaChannel() {
        return ch;
    }

    // 获取该channel绑定的唯一SelectionKey
    protected SelectionKey selectionKey() {
        assert selectionKey != null;
        return selectionKey;
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }


    interface NioUnsafe extends Unsafe {
        // 获取该channel的java原生Channel
        SelectableChannel ch();

        // 完成连接的结尾工作
        void finishConnect();

        // read放到具体的netty NIO channel中去实现，不同的NIOChannel，read逻辑是不一样的
        // 抽象方法，子类实现，完成read
        // * 客户端Channel"读"事件处理逻辑：读取IO流数据，然后触发后续处理
        // * 服务端Channel"读"事件处理逻辑：接受客户端连接，生成服务端侧的客户端Channel，并做后续的处理
        void read();

        void forceFlush();
    }

    /**
     * NioUnsafe的抽象内部类
     * 内部类（非静态）可以访问外部类的所有成员变量和方法，自动持有对外部类实例的引用
     */
    protected abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {

        @Override
        public final SelectableChannel ch() {
            return javaChannel();
        }

        @Override
        public final void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            try {
                boolean doConnect = doConnect(remoteAddress, localAddress);
                if (!doConnect) {
                    //这里的代码会搞出一个bug，我会在第六个版本的代码中修正，同时也会给大家讲一下bug是怎么产生的。这个bug只会在收发数据时
                    //体现出来，所以并不会影响我们本节课的测试。我们现在还没有开始收发数据
                    promise.trySuccess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public final void finishConnect() {
            assert eventLoop().inEventLoop(Thread.currentThread());
            try {
                //真正处理连接完成的方法
                doFinishConnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public final void forceFlush() {
        }

    }

    /**
     * 注册的行为很简单，也很通用，其实就是注册channel到selector上，所以就在这一层统一实现掉
     *
     * @throws Exception
     */
    @Override
    protected void doRegister() throws Exception {
        //在这里把channel注册到单线程执行器中的selector上,注意这里的第三个参数this，这意味着channel注册的时候把本身，当作附件放到key上了，
        //之后在EventLoop中处理selectionKey时，就能拿到这个具体实现类了，就能用这个实现类来响应处理该selectionKey事件了，那么EventLoop就解耦掉处理不同事件的具体实现了。
        //还有这里注册的事件是0，也就是不关注任何事件，目的其实就是将channel绑定到selector，获取到唯一的SelectionKey，真正的感兴趣事件会在beginRead()这个方法中完成添加
        selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
    }

    @Override
    protected void doBeginRead() throws Exception {
        final SelectionKey selectionKey = this.selectionKey;
        //检查key是否是有效的
        if (!selectionKey.isValid()) {
            return;
        }
        //还没有设置感兴趣的事件，所以得到的值为0；如果设置了感兴趣的事件，那么得到的值就是设置的值
        final int interestOps = selectionKey.interestOps();
        //如果interestOps中并不包含readInterestOp，说明尚未设置该事件，那么设置该事件
        if ((interestOps & readInterestOp) == 0) {
            //设置channel关注的事件，读事件
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }

    @Override
    protected void doClose() throws Exception {

    }

    /**
     * 抽象方法，子类实现，完成connect
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     * @throws Exception
     */
    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;

    /**
     * 抽象方法，子类实现，完成finishConnect
     *
     * @throws Exception
     */
    protected abstract void doFinishConnect() throws Exception;

}
