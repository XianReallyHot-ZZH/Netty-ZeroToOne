package com.yy.netty.channel.socket;

import com.yy.netty.channel.nio.AbstractNioMessageChannel;
import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.util.internal.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

/**
 * 服务端channel的最终实现类
 * 对serversocketchannel做了一层包装，同时也因为channel接口和抽象类的引入，终于可以使NioEventLoop和channel解耦了
 */
public class NioServerSocketChannel extends AbstractNioMessageChannel {

    //在无参构造器被调用的时候，该成员变量就被创建了
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    // 生成一个ServerSocketChannel
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a server socket.", e);
        }
    }

    public NioServerSocketChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    public NioServerSocketChannel(ServerSocketChannel channel) {
        super(null, channel, SelectionKey.OP_ACCEPT);
    }

    @Override
    public boolean isActive() {
        return isOpen() && javaChannel().socket().isBound();
    }

    @Override
    protected ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    @Override
    protected SocketAddress localAddress0() {
        return SocketUtils.localSocketAddress(javaChannel().socket());
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }


    /**
     * 这里做空实现即可，服务端的channel并不会做连接动作
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     * @throws Exception
     */
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        //这里是一个系统调用方法，判断当前的java版本是否为7以上，这里我就直接写死了，不引入更多的工具类了
        //如果用户没有设置backlog参数，config.getBacklog()点进去看源码最后发现会该值会在NetUtil的静态代码块中被赋值，windows环境下值为200
        //linux环境下默认为128。Backlog可以设置全连接队列的大小，控制服务端接受连接的数量。
        //这里就直接写死了，还没有引入channelconfig配置类
        javaChannel().bind(localAddress, 128);
        if (isActive()) {
            System.out.println("服务端绑定端口成功");
            // 注册“读”事件，开始接受客户端的连接
            doBeginRead();
        }
    }

    protected void doClose() throws Exception {
        javaChannel().close();
    }

    /**
     * 服务端channel的“读”事件处理逻辑,接受来自客户端的连接
     *
     * @param buf
     * @return
     */
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        //有连接进来，创建出java原生的客户端channel
        SocketChannel ch = SocketUtils.accept(javaChannel());
        try {
            if (ch != null) {
                //创建客户端niosocketchannel
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                //有异常则关闭客户端的channel
                ch.close();
            } catch (Throwable t2) {
                throw new RuntimeException("Failed to close a socket.", t2);
            }
        }

        return 0;
    }

}
