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

    // 持有该channel的unsafe（channel某些方法或者能力的非安全实现）
    private final Unsafe unsafe;

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
        unsafe = newUnsafe();
        id = newId();
    }

    // 带id的构造函数
    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        this.id = id;
        unsafe = newUnsafe();
    }

    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    // --------------------------------------------- channel 接口实现 ---------------------------------------------

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

    // 得到本地地址
    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                // 尝试从unsafe中获取
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }
        return localAddress;
    }

    // 得到远程地址
    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }
        return remoteAddress;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public Channel read() {
        unsafe.beginRead();
        return this;
    }

    @Override
    public Channel flush() {
        return null;
    }

    protected abstract AbstractUnsafe newUnsafe();

    // ------------------------------------------ ChannelOutboundInvoker 接口实现 ------------------------------------------

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        // 调用unsafe的bind方法完成绑定操作
        unsafe.bind(localAddress, promise);
        return promise;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        // 调用unsafe的connect方法完成连接操作
        unsafe.connect(remoteAddress, localAddress, promise);
        return promise;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        DefaultChannelPromise promise = new DefaultChannelPromise(this);
        unsafe.write(msg, promise);
        return promise;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }


    // ------------------------------------------ AbstractChannel 抽象类实现 ------------------------------------------

    /**
     * Unsafe的抽象内部类
     * 看到下面一下子多了那么多方法，而且还有很多是没实现的，一定会感到很懵逼
     * 但是，请记住这句话，在抽象类中，很多方法尽管实现了方法体，内在逻辑却并不复杂。因为这些方法几乎都是为子类服务的，
     * 换句话说，就是定义了一些模版方法而已，真正实现的方法在子类之中。就比如调用了AbstractChannel抽象类的bind方法，
     * 在该方法内部，又会调用unsafe抽象内部类的bind方法，而该内部类的bind方法又会调用AbstractChannel类中的另一个抽象方法
     * doBind，虽然NioServerChannel中也有该方法的实现，但该方法在子类NioServerSocketChannel中才是真正实现。
     * 我想，这时候有的朋友可能又会困惑作者为什么这样设计类的继承结构。也许有的朋友已经清楚了，但我在这里再啰嗦一句，
     * 首先，channel分为客户端和服务端，因为抽象出了公共的接口和父抽象类，两种channel不得不实现相同的方法，
     * 那么不同的channel实现的相同方法的逻辑应该不同，所以dobind设计为抽象方法是很合理的。因为你不能让NiosocketChannel客户端channel
     * 向服务端channel那样去绑定端口，虽然要做也确实可以这么做。。
     */
    protected abstract class AbstractUnsafe implements Unsafe {

        // 内部类（非静态）可以访问外部类的所有成员变量和方法，自动持有对外部类实例的引用

        private void assertEventLoop() {
            // 检查当前线程是否是netty eventLoop线程
            assert !registered || eventLoop().inEventLoop(Thread.currentThread());
        }

        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return remoteAddress0();
        }

        /**
         * <p>核心方法!!!</p>
         * <p>
         * channel抽象层最核心的一个实现方法，注册这一行为可以认为是channel这一层的通用需求，可以在这里进行统一的默认实现
         * 效果：将channel注册到指定的EventLoop，其实就是将本channel和EventLoop里的selector进行绑定
         * </p>
         *
         * @param eventLoop 指定的EventLoop
         * @param promise   和本次注册逻辑相关的ChannelPromise协调器
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
        public final void deregister(final ChannelPromise promise) {
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
        public final void disconnect(ChannelPromise promise) {

        }

        @Override
        public final void close(ChannelPromise promise) {

        }

        @Override
        public final void closeForcibly() {
            assertEventLoop();

            try {
                doClose();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        @Override
        public final void beginRead() {
            assertEventLoop();

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

        @Override
        public final void write(Object msg, ChannelPromise promise) {
            try {
                // 调用子类的写方法
                doWrite(msg);
                //如果有监听器，这里可以通知监听器执行回调方法
                promise.trySuccess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public final void flush() {

        }

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

    }

    protected abstract SocketAddress localAddress0();

    protected abstract SocketAddress remoteAddress0();

    /**
     * 检查指定的EventLoop是否能兼容到当前channel的注册具体实现
     *
     * @param eventLoop
     * @return
     */
    protected abstract boolean isCompatible(EventLoop eventLoop);

    /**
     * 由子类来具体实现，具体的写方法实现，具体实现涉及到NIO相关类使用，放到具体的业务子类去实现
     *
     * @param msg
     * @throws Exception
     */
    protected abstract void doWrite(Object msg) throws Exception;

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
     *
     * @throws Exception
     */
    protected abstract void doBeginRead() throws Exception;

    /**
     * 由子类来具体实现，具体的关闭实现，具体实现涉及到NIO相关类使用，放到具体的业务子类去实现
     *
     * @throws Exception
     */
    protected abstract void doClose() throws Exception;


    private ClosedChannelException newClosedChannelException(Throwable cause) {
        ClosedChannelException exception = new ClosedChannelException();
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
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
