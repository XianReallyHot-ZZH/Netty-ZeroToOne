package com.yy.netty.channel.socket.nio;

import com.yy.netty.channel.ChannelOption;
import com.yy.netty.channel.nio.AbstractNioMessageChannel;
import com.yy.netty.channel.nio.NioEventLoop;
import com.yy.netty.channel.socket.DefaultServerSocketChannelConfig;
import com.yy.netty.channel.socket.ServerSocketChannelConfig;
import com.yy.netty.util.internal.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.Map;

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

    // 服务端channel的配置类
    private final ServerSocketChannelConfig config;

    public NioServerSocketChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    public NioServerSocketChannel(ServerSocketChannel channel) {
        super(null, channel, SelectionKey.OP_ACCEPT);
        // 创建服务端channel的配置类
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }

    @Override
    public ServerSocketChannelConfig config() {
        return config;
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
        //现在，我们可以把这里换成用户配置的参数了
        javaChannel().bind(localAddress, config.getBacklog());
        System.out.println("服务端channel绑定端口，并设置backlog参数为：" + config.getBacklog());
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

    @Override
    protected void doFinishConnect() throws Exception {
        // 服务端不做处理
        throw new UnsupportedOperationException();
    }

    /**
     * 引入该内部类，该内部类最终会把用户配置的channel参数真正传入jdk的channel中
     */
    private final class NioServerSocketChannelConfig extends DefaultServerSocketChannelConfig {

        private NioServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
            super(channel, javaSocket);
        }

        /**
         * 重写setOption方法，支持服务端NIO原生channel配置项的设置
         *
         * @param option
         * @param value
         * @param <T>
         * @return
         */
        @Override
        public <T> boolean setOption(ChannelOption<T> option, T value) {
            if (option instanceof NioChannelOption) {
                //把用户设置的参数传入原生的jdk的channel中
                return NioChannelOption.setOption(jdkChannel(), (NioChannelOption<T>) option, value);
            }
            //正常调用的话，该方法的逻辑会走到这个分支处
            return super.setOption(option, value);
        }

        /**
         * 重写getOption方法，支持服务端NIO原生channel配置项的获取
         *
         * @param option
         * @param <T>
         * @return
         */
        @Override
        public <T> T getOption(ChannelOption<T> option) {
            //这里有一行代码，判断jdk版本是否大于7，我就直接删掉了，默认大家用的都是7以上，否则要引入更多工具类
            if (option instanceof NioChannelOption) {
                return NioChannelOption.getOption(jdkChannel(), (NioChannelOption<T>) option);
            }
            return super.getOption(option);
        }

        /**
         * 重写getOptions方法，支持服务端NIO原生channel配置项的获取
         *
         * @return
         */
        @SuppressWarnings("unchecked")
        @Override
        public Map<ChannelOption<?>, Object> getOptions() {
            return getOptions(super.getOptions(), NioChannelOption.getOptions(jdkChannel()));
        }

        /**
         * @Author: PP-jessica
         * @Description: 这个方法得到的就是jdk的channel
         */
        private ServerSocketChannel jdkChannel() {
            return ((NioServerSocketChannel) channel).javaChannel();
        }


    }

}
