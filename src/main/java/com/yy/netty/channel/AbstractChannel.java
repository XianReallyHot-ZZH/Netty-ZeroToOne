package com.yy.netty.channel;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

/**
 * channel的第一层抽象类，会实现一些channel这个层级下通用的方法、持有一些通用的成员变量
 */
public abstract class AbstractChannel implements Channel {

    /**
     * 父channel
     * 只有在服务端生成的客户端socketChannel才会有父channel
     * 服务端生成的serverSocketChannel和客户端生成的socketChannel都不会有父channel
     */
    private final Channel parent;

    // 本channel的id
    private final ChannelId id;

    // 这个future是在channel关闭的时候使用的，是一个静态内部类
    private final CloseFuture closeFuture = new CloseFuture(this);

    // channel关联的本地服务端口
    private volatile SocketAddress localAddress;

    // channel关联的远程服务端口
    private volatile SocketAddress remoteAddress;

    // 保存的初始化关闭的异常
    private Throwable initialCloseCause;

    // channel关联的EventLoop,每一个channel有且只绑定到一个eventloop上，因为一个EventLoop内含有一个selector，一个channel只能绑定到一个selector上
    private volatile EventLoop eventLoop;

    // channel是否已经注册到EventLoop
    private volatile boolean registered;

    // 构造函数
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
    }

    // 带id的构造函数
    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        this.id = id;
    }

    @Override
    public ChannelId id() {
        return this.id;
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }


    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    /**
     * 检查指定的EventLoop是否能兼容到当前channel的注册具体实现
     *
     * @param eventLoop
     * @return
     */
    protected abstract boolean isCompatible(EventLoop eventLoop);

    /**
     * <p>核心方法!!!</p>
     * <p>
     * channel抽象层最核心的一个实现方法，注册这一行为可以认为是channel这一层的通用需求，可以在这里进行统一的默认实现
     * 效果：将channel注册到指定的EventLoop，其实就是将本channel和EventLoop里的selector进行绑定
     * </p>
     *
     * @param eventLoop     指定的EventLoop
     * @param promise       和本次注册逻辑相关的ChannelPromise协调器
     */
    @Override
    public final void register(EventLoop eventLoop, ChannelPromise promise) {
        if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
        }

        //检查channel是否注册过，注册过就不用再次注册了，手动将这一次注册尝试设置为promise失败
        if (isRegistered()) {
            promise.setFailure(new IllegalStateException("registered to an event loop already"));
            return;
        }

        //判断当前使用的执行器是否为NioEventLoop，如果不是手动设置失败
        if (!isCompatible(eventLoop)) {
            promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
            return;
        }

        // 学过netty的人都知道，一个channel绑定一个单线程执行器。
        // 不管是客户端还是服务端的，会把自己注册到绑定的单线程执行器中的selector上
        AbstractChannel.this.eventLoop = eventLoop;
        //又看到这个方法了，又一次说明在netty中，channel注册，绑定，连接等等都是异步的，由单线程执行器来执行
        if (eventLoop.inEventLoop(Thread.currentThread())) {
            register0(promise);
        } else {
            try {
                //如果调用该放的线程不是netty的线程，就封装成任务由线程执行器来执行
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        register0(promise);
                    }
                });
            } catch (Throwable t) {
                System.out.println(t.getMessage());
                //进行强制close，该方法先不做实现，等引入unsafe之后会实现
                //closeForcibly();
                // close协调器设置失败
                closeFuture.setClosed();
                // 给传入的promise设置失败
                safeSetFailure(promise, t);
            }
        }
    }

    /**
     * <p>核心方法!!!</p>
     * <p>
     * register注册的具体实现
     * </p
     *
     * @param promise
     */
    private void register0(ChannelPromise promise) {
        try {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                // 如果不能确保channel是打开的或者不能确保promise是不可取消的，那么直接结束了
                return;
            }
            //真正的注册方法
            doRegister();
            //走到这这里，说明成功了，修改注册状态
            registered = true;
            //把成功状态赋值给promise，这样它可以通知回调函数执行
            //我们在之前注册时候，把bind也放在了回调函数中
            safeSetSuccess(promise);
            //在这里给channel注册“读”事件
            beginRead();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public final void bind(SocketAddress localAddress, ChannelPromise promise) {
        try {
            doBind(localAddress);
            safeSetSuccess(promise);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void beginRead() {
        //注册“读”事件是有前置条件的，不是随便让你注册的，比如服务端channel只有绑定端口号成功之后，才能注册“读”事件
        //所以这里要进行条件判断，如果不符合是要提前返回的，不会真正去完成读事件的注册。
        //最终我们会通过一些回调机制手段，比如服务端channel真正完成端口绑定后，会再次回调到这里，完成读事件的注册
        if (!isActive()) {
            return;
        }

        try {
            doBeginRead();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 由子类来具体实现，具体的注册实现涉及到NIO相关类的使用了，我们把这部分放到NioChannel抽象类中去实现
     *
     * @throws Exception
     */
    protected abstract void doRegister() throws Exception;

    /**
     * 由子类来具体实现，具体的绑定实现涉及到NIO相关类的使用了，然后服务端和客户端的端口绑定行为是不一样的，放到具体的业务子类去实现
     *
     * @param localAddress
     * @throws Exception
     */
    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    /**
     * 由子类来具体实现，具体的读事件注册实现，具体实现涉及到NIO相关类使用，放到具体的业务子类去实现
     * @throws Exception
     */
    protected abstract void doBeginRead() throws Exception;

    /**
     * 确保channel是打开的，如果不是，那么设置promise失败
     *
     * @param promise
     * @return
     */
    protected final boolean ensureOpen(ChannelPromise promise) {
        if (isOpen()) {
            return true;
        } else {
            // channel没有打开，设置promise失败
            safeSetFailure(promise, newClosedChannelException(initialCloseCause));
            return false;
        }
    }

    private ClosedChannelException newClosedChannelException(Throwable cause) {
        ClosedChannelException exception = new ClosedChannelException();
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    protected final void safeSetSuccess(ChannelPromise promise) {
        if (!promise.trySuccess()) {
            System.out.println("Failed to mark a promise as success because it is done already: " + promise);
        }
    }


    protected final void safeSetFailure(ChannelPromise promise, Throwable t) {
        if (!promise.tryFailure(t)) {
            throw new RuntimeException(t);
        }
    }

    /**
     * 关闭的future，用于和close逻辑进行协调
     */
    static final class CloseFuture extends DefaultChannelPromise {

        public CloseFuture(AbstractChannel channel) {
            super(channel);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }

    }

}
