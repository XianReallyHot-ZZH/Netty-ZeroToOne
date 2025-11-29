package com.yy.netty.channel.socket;

import com.yy.netty.channel.Channel;
import com.yy.netty.channel.nio.AbstractNioByteChannel;
import com.yy.netty.util.internal.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * 客户端channel的最终实现类
 * 对socketchannel做了一层包装，同时也因为channel接口和抽象类的引入，终于可以使NioEventLoop和channel解耦了
 */
public class NioSocketChannel extends AbstractNioByteChannel {

    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();


    private static SocketChannel newSocket(SelectorProvider provider) {
        try {
            return provider.openSocketChannel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a socket.", e);
        }
    }

    public NioSocketChannel() {
        this(DEFAULT_SELECTOR_PROVIDER);
    }

    public NioSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    public NioSocketChannel(SocketChannel socket) {
        this(null, socket);
    }

    public NioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
    }

    @Override
    protected SocketChannel javaChannel() {
        return (SocketChannel) super.javaChannel();
    }

    @Override
    public boolean isActive() {
        //channel是否为Connected状态，是客户端channel判断是否激活的条件。
        SocketChannel ch = javaChannel();
        return ch.isOpen() && ch.isConnected();
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
        return javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return javaChannel().socket().getRemoteSocketAddress();
    }


    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        doBind0(localAddress);
    }

    private void doBind0(SocketAddress localAddress) throws Exception {
        SocketUtils.bind(javaChannel(), localAddress);
    }

    /**
     * 客户端channel的doConnect是必须要实现的，因为客户端channel是要去连接服务器的，所以需要实现connect方法
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     * @throws Exception
     */
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        //从这里可以看出，如果连接的时候把本地地址也传入了，那么就要在连接远端的同时，监听本地端口号
        if (localAddress != null) {
            doBind0(localAddress);
        }

        boolean success = false;
        try {
            //SocketUtils.connect(javaChannel(), remoteAddress)该方法如果连接成功就直接返回true
            //如果没有接收到服务端的ack就会返回false，但并不意味着该方法就彻底失败了，有可能ack在路上等等，最终需要注册连接事件来监听结果
            //这会让AbstractNioChannel类中的connect方法进入到添加定时任务的分支，如果超过设定的时间一直没有连接成功，就会在客户端报错
            //如果连接成功了，连接成功的时候会把该定时任务中的某些变量置为null,现在我们还没有加入定时任务
            //这里会返回false
            boolean connected = SocketUtils.connect(javaChannel(), remoteAddress);
            if (!connected) {
                //如果连接没有成功，那么就注册连接事件，并返回false，后续在EventLoop中处理到连接成功事件时，会为该Channel注册真正的读事件
                selectionKey().interestOps(SelectionKey.OP_CONNECT);
            }
            success = true;
            return connected;
        } finally {
            if (!success) {
                doClose();
            }
        }
    }

    @Override
    protected int doReadBytes(ByteBuffer byteBuf) throws Exception {
        int len = javaChannel().read(byteBuf);
        byte[] buffer = new byte[len];
        byteBuf.flip();
        byteBuf.get(buffer);
        System.out.println("客户端收到消息:{}" + new String(buffer));
        //返回读取到的字节长度
        return len;
    }

    protected void doClose() throws Exception {
        javaChannel().close();
    }

    @Override
    protected void doWrite(Object msg) throws Exception {
        //真正发送数据的时候到了，这时候就不能用NioSocketChannel了，要用java原生的socketchannel
        SocketChannel socketChannel = javaChannel();
        //转换数据类型
        ByteBuffer buffer = (ByteBuffer)msg;
        //发送数据
        socketChannel.write(buffer);
        //因为在我们自己的netty中，客户端的channel连接到服务端后，并没有绑定单线程执行器呢，所以即便发送了数据也收不到
        //但我们可以看看客户端是否可以发送成功，证明我们的发送逻辑是没问题的，
        // 接收数据的验证，放到引入channelhandler之后再验证
        System.out.println("[MOCK]客户端发送数据成功了！");
    }

}
