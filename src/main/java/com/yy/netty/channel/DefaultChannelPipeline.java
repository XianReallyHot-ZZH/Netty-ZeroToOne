package com.yy.netty.channel;

import com.yy.netty.util.concurrent.EventExecutor;
import com.yy.netty.util.concurrent.EventExecutorGroup;
import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;

/**
 * 默认的ChannelPipeline实现类,定位为整个链表pipeline的顶层工具类，为用户提供各种链表上业务流程触发操作+链表查询维护操作
 */
public class DefaultChannelPipeline implements ChannelPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelPipeline.class);

    // 默认头结点的名称
    private static final String HEAD_NAME = generateName0(HeadContext.class);
    // 默认尾结点的名称
    private static final String TAIL_NAME = generateName0(TailContext.class);

    // key: ChannelHandler的name， value: ChannelHandler
    private static final ThreadLocal<Map<Class<?>, String>> nameCaches =
            //nameCaches中缓存着每个ChannelHandler的名字
            new ThreadLocal<Map<Class<?>, String>>() {
                @Override
                protected Map<Class<?>, String> initialValue() {
                    return new WeakHashMap<Class<?>, String>();
                }
            };

    // 把DefaultChannelPipeline当成一个链表的话，下面两个就是头节点和尾节点
    // 头结点
    final AbstractChannelHandlerContext head;
    // 尾结点
    final AbstractChannelHandlerContext tail;

    // pipeline对应的 channel
    // channel本身就是一个map，这意味着用户向channel中存储了数据，那么只要通过DefaultChannelPipeline得到了channel，就可以得到channel中的数据,
    // 方便用户在实现ChannelHandler获取到数据
    private final Channel channel;

    private ChannelFuture succeededFuture;

    // 为true则是第一次注册
    private boolean firstRegistration = true;

    // Channel是否注册成功，这里指的是是否注册单线程执行器成功
    private boolean registered;

    // 这是一个非常重要的任务链表，在向DefaultChannelPipeline中添加handler时，会用到这个链表,根据变量的名字可以知道，这明显是链表的头节点
    private PendingHandlerCallback pendingHandlerCallbackHead;


    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        //因为我没有引入CompleteFuture，所以这一行先注释了
        //succeededFuture = new DefaultChannelPromise(channel, null);
        tail = new TailContext(this);
        head = new HeadContext(this);
        head.next = tail;
        tail.prev = head;
    }


    private static String generateName0(Class<?> handlerType) {
        return StringUtil.simpleClassName(handlerType) + "#0";
    }

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public List<String> names() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        return Collections.emptyMap();
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelHandler remove(String name) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandler removeFirst() {
        return null;
    }

    @Override
    public ChannelHandler removeLast() {
        return null;
    }

    @Override
    public ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
        return null;
    }

    @Override
    public ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler) {
        return null;
    }

    @Override
    public ChannelHandler first() {
        return null;
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return null;
    }

    @Override
    public ChannelHandler last() {
        return null;
    }

    @Override
    public ChannelHandlerContext lastContext() {
        return null;
    }

    @Override
    public ChannelHandler get(String name) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(String name) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRead(Object msg) {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(Object event) {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
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
        return null;
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
    public ChannelOutboundInvoker read() {
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
    public ChannelPipeline flush() {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return null;
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

    @Override
    public Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        return null;
    }


    final void invokeHandlerAddedIfNeeded() {
        assert channel.eventLoop().inEventLoop(Thread.currentThread());
        //这里是为了保证下面的方法只被执行一次
        if (firstRegistration) {
            firstRegistration = false;
            //还记得这个PendingHandlerAddedTask吧？这里就要开始执行它的run方法，然后回调每一个ChannelHandler的handlerAdded方法
            callHandlerAddedForAllHandlers();
        }
    }

    private void callHandlerAddedForAllHandlers() {
        //回调任务链表的头节点
        final PendingHandlerCallback pendingHandlerCallbackHead;
        synchronized (this) {
            assert !registered;
            registered = true;
            pendingHandlerCallbackHead = this.pendingHandlerCallbackHead;
            //帮助垃圾回收
            this.pendingHandlerCallbackHead = null;
        }
        PendingHandlerCallback task = pendingHandlerCallbackHead;
        // 挨个执行任务列表中的任务
        while (task != null) {
            task.execute();
            task = task.next;
        }
    }

    /**
     * 调用传入的AbstractChannelHandlerContext节点内的ChannelHandler的handlerAdded方法
     * @param ctx
     */
    private void callHandlerAdded0(final AbstractChannelHandlerContext ctx) {
        try {
            //开始执行回调任务了
            ctx.callHandlerAdded();
        } catch (Throwable t) {
            boolean removed = false;
            try {
                remove0(ctx);
                ctx.callHandlerRemoved();
                removed = true;
            } catch (Throwable t2) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to remove a handler: " + ctx.name(), t2);
                }
            }
            if (removed) {
                // 触发pipeline的异常处理流程
                fireExceptionCaught(new ChannelPipelineException(ctx.handler().getClass().getName() + ".handlerAdded() has thrown an exception; removed.", t));
            } else {
                fireExceptionCaught(new ChannelPipelineException(ctx.handler().getClass().getName() + ".handlerAdded() has thrown an exception; also failed to remove.", t));
            }
        }
    }

    /**
     * 调用传入的AbstractChannelHandlerContext节点内的ChannelHandler的HandlerRemoved方法
     * @param ctx
     */
    private void callHandlerRemoved0(final AbstractChannelHandlerContext ctx) {
        try {
            ctx.callHandlerRemoved();
        } catch (Throwable t) {
            // 触发pipeline的异常处理流程
            fireExceptionCaught(new ChannelPipelineException(ctx.handler().getClass().getName() + ".handlerRemoved() has thrown an exception.", t));
        }
    }

    /**
     * 在当前pipeline上删除指定的节点
     * @param ctx
     */
    private static void remove0(AbstractChannelHandlerContext ctx) {
        AbstractChannelHandlerContext prev = ctx.prev;
        AbstractChannelHandlerContext next = ctx.next;
        prev.next = next;
        next.prev = prev;
    }

    /**
     * 创建并保存一个待处理的任务,待处理任务会保存在pendingHandlerCallbackHead中,以链表的形式保存
     *
     * @param ctx
     * @param added
     */
    private void callHandlerCallbackLater(AbstractChannelHandlerContext ctx, boolean added) {
        assert !registered;
        //如果是添加节点就创建PendingHandlerAddedTask对象，删除节点就创建PendingHandlerRemovedTask节点
        PendingHandlerCallback task = added ? new PendingHandlerAddedTask(ctx) : new PendingHandlerRemovedTask(ctx);
        PendingHandlerCallback pending = pendingHandlerCallbackHead;
        //如果链表还没有头节点，就把创建的对象设成头节点
        if (pending == null) {
            pendingHandlerCallbackHead = task;
        } else {
            //如果有头节点了，就把节点依次向后添加
            while (pending.next != null) {
                pending = pending.next;
            }
            // 添加到链表末尾
            pending.next = task;
        }
    }

    /**
     * 移除pipeline上所有已添加的ChannelHandler
     * 1、双向遍历策略：先向后遍历到尾部，再向前遍历到头部，确保所有 Handler 都能正确地被移除
     * 2、线程安全：通过线程检查和同步机制确保在正确的事件循环线程中执行移除操作
     * 3、资源清理：通过 handlerRemoved 回调让每个 Handler 能够完成自己的清理工作
     * 确保了在 Channel 关闭时，Pipeline 中的所有 Handler 都能被正确地移除和清理，避免内存泄漏和状态不一致的问题
     */
    private synchronized void destroy() {
        destroyUp(head.next, false);
    }

    /**
     * 首先找到链表末尾的节点，然后从末尾开始调用destroyDown方法开始向前遍
     * 【向后遍历】
     *
     * @param ctx
     * @param inEventLoop
     */
    private void destroyUp(AbstractChannelHandlerContext ctx, boolean inEventLoop) {
        final Thread currentThread = Thread.currentThread();
        final AbstractChannelHandlerContext tail = this.tail;
        for (;;) {
            if (ctx == tail) {
                destroyDown(currentThread, tail.prev, inEventLoop);
                break;
            }
            final EventExecutor executor = ctx.executor();
            if (!inEventLoop && !executor.inEventLoop(currentThread)) {
                final AbstractChannelHandlerContext finalCtx = ctx;
                // 运行在非事件循环线程，则运行在事件循环线程
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        destroyUp(finalCtx, true);
                    }
                });
                // 在上面异步掉后，当前线程退出
                break;
            }
            // 往后找
            ctx = ctx.next;
            inEventLoop = false;
        }
    }

    /**
     * 调用remove0，从pipeline中物理移除指定的节点，然后往前找，直到找到链表头节点（略过头结点）
     * 【向前遍历】
     *
     * @param currentThread
     * @param ctx
     * @param inEventLoop
     */
    private void destroyDown(Thread currentThread, AbstractChannelHandlerContext ctx, boolean inEventLoop) {
        final AbstractChannelHandlerContext head = this.head;
        for (;;) {
            if (ctx == head) {
                break;
            }
            final EventExecutor executor = ctx.executor();
            if (inEventLoop || executor.inEventLoop(currentThread)) {
                synchronized (this) {
                    // 从pipeline上移除节点
                    remove0(ctx);
                }
                callHandlerRemoved0(ctx);
            } else {
                final AbstractChannelHandlerContext finalCtx = ctx;
                // 运行在非事件循环线程，则运行在事件循环线程
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        destroyDown(Thread.currentThread(), finalCtx, true);
                    }
                });
                break;
            }

            // 往前找
            ctx = ctx.prev;
            inEventLoop = false;
        }
    }

    /**
     * 在这个方法中给channel绑定读事件
     * 服务端就是监听客户端的连接请求，客户端就是监听服务端的响应请求
     */
    private void readIfIsAutoRead() {
        if (channel.config().isAutoRead()) {
            // 如果配置了自动触发“读”，那么就调用channel的read方法，不然是需要手动调用的
            channel.read();
        }
    }


    protected void onUnhandledInboundMessage(ChannelHandlerContext ctx, Object msg) {
        onUnhandledInboundMessage(msg);
        if (logger.isDebugEnabled()) {
            logger.debug("Discarded message pipeline : {}. Channel : {}.", ctx.pipeline().names(), ctx.channel());
        }
    }

    protected void onUnhandledInboundMessage(Object msg) {
        try {
            logger.debug("Discarded inbound message {} that reached at the tail of the pipeline. Please check your pipeline configuration.", msg);
        } finally {
            //当该引用计数减至为0时，该ByteBuf即可回收，待后面实现
            //ReferenceCountUtil.release(msg);
        }
    }

    protected void onUnhandledInboundChannelReadComplete() {
    }

    protected void onUnhandledInboundUserEventTriggered(Object evt) {
        // This may not be a configuration error and so don't log anything.
        // The event may be superfluous for the current pipeline configuration.
        //ReferenceCountUtil.release(evt);
    }


    protected void onUnhandledChannelWritabilityChanged() {
    }

    protected void onUnhandledInboundException(Throwable cause) {
        try {
            logger.warn("An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.", cause);
        } finally {
            //ReferenceCountUtil.release(cause);
        }
    }

    protected void onUnhandledInboundChannelActive() {
    }


    protected void onUnhandledInboundChannelInactive() {
    }

    /**
     * 头节点即是出站处理器，又是入站处理器,本身也是链表节点
     * 1、最终所有的出站操作的终点就是这里，无论如何最终都会回归到这里
     * 2、所有入站操作的起点都是这里,逻辑很简单，就触发当前节点的在链表上的下一个节点的入站方法，因为头结点是没有入站逻辑的，只有触发下一个节点的逻辑
     */
    final class HeadContext extends AbstractChannelHandlerContext implements ChannelOutboundHandler, ChannelInboundHandler {

        private final Channel.Unsafe unsafe;

        HeadContext(DefaultChannelPipeline pipeline) {
            super(pipeline, null, HEAD_NAME, HeadContext.class);
            unsafe = pipeline.channel().unsafe();
            //设置channelHandler的状态为ADD_COMPLETE，说明该节点添加之后直接就可以处理数据
            setAddComplete();
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        // ------------------------------ ChannelHandler接口方法 ------------------------------
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {}

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {}

        // ------------------------------ ChannelOutboundHandler出站接口方法 ------------------------------
        @Override
        public void read(ChannelHandlerContext ctx) throws Exception {
            unsafe.beginRead();
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            //调用unsafe的方法，然后就是老样子了，再一路调用到NioServerSocketChannel中
            unsafe.bind(localAddress, promise);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            unsafe.write(msg, promise);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            unsafe.flush();
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            unsafe.connect(remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            unsafe.disconnect(promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            unsafe.close(promise);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            unsafe.deregister(promise);
        }

        // ------------------------------ ChannelInboundHandler入站接口方法 ------------------------------
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 触发channel读流程（信息处理流程）
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelReadComplete();
            readIfIsAutoRead();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            invokeHandlerAddedIfNeeded();
            ctx.fireChannelRegistered();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelUnregistered();
            if (!channel.isOpen()) {
                destroy();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelActive();
            //在这个方法中给channel绑定读事件，跟随者调用链点到这里，应该能明白，不管是客户端还是服务端的channel，它们
            //触发channel绑定读事件（服务端开始接受客户端连接，客户端开始接受信息读），都是在channelActive被调用完后执行
            readIfIsAutoRead();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelInactive();
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelWritabilityChanged();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.fireExceptionCaught(cause);
        }
    }

    /**
     * 尾节点是个入站处理器,不是出站处理器
     * 1、只要不是用户在ChannelHandler中主动调用出站方法，所有入站操作的终点都是这里,作为入站终点，啥都不用做，到这里结束链表的串行调用即可
     * 2、可以用来从尾结点作为起点往前找出站处理器，那么相当于不会遗漏掉任何出站处理器
     */
    final class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler {

        TailContext(DefaultChannelPipeline pipeline) {
            super(pipeline, null, TAIL_NAME, TailContext.class);
            setAddComplete();
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        // ------------------------------ ChannelHandler接口方法 ------------------------------
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {}

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {}

        // ------------------------------ ChannelInboundHandler入站接口方法 ------------------------------
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //如果接收到的msg传到了尾节点，说明该数据没有被处理过，这里直接释放内存即可
            onUnhandledInboundMessage(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            onUnhandledInboundChannelReadComplete();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            onUnhandledInboundUserEventTriggered(evt);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            onUnhandledInboundChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            onUnhandledInboundChannelInactive();
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            onUnhandledChannelWritabilityChanged();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            onUnhandledInboundException(cause);
        }
    }


    /**
     * 单向链表中的节点，内部持有一个channel pipeline上的一个节点，是一对一的关系
     */
    private abstract static class PendingHandlerCallback implements Runnable {
        // channel pipeline上的一个节点
        final AbstractChannelHandlerContext ctx;
        // HandlerCallback链表的下一个节点
        PendingHandlerCallback next;

        PendingHandlerCallback(AbstractChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        abstract void execute();
    }

    /**
     * 待执行回调指定AbstractChannelHandlerContext节点HandlerAdded的任务
     */
    private final class PendingHandlerAddedTask extends PendingHandlerCallback {

        PendingHandlerAddedTask(AbstractChannelHandlerContext ctx) {
            super(ctx);
        }

        // 触发AbstractChannelHandlerContext内的HandlerAdded
        @Override
        public void run() {
            callHandlerAdded0(ctx);
        }

        // 触发AbstractChannelHandlerContext内的HandlerAdded
        @Override
        void execute() {
            EventExecutor executor = ctx.executor();
            if (executor.inEventLoop(Thread.currentThread())) {
                callHandlerAdded0(ctx);
            } else {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Can't invoke handlerAdded() as the EventExecutor {} rejected it, removing handler {}.", executor, ctx.name(), e);
                    }
                    // 从pipeline上移除当前节点
                    remove0(ctx);
                    ctx.setRemoved();
                }
            }
        }
    }

    /**
     * 待执行回调指定AbstractChannelHandlerContext节点HandlerRemoved的任务
     */
    private final class PendingHandlerRemovedTask extends PendingHandlerCallback {

        PendingHandlerRemovedTask(AbstractChannelHandlerContext ctx) {
            super(ctx);
        }

        // 触发AbstractChannelHandlerContext内的HandlerRemoved
        @Override
        public void run() {
            callHandlerRemoved0(ctx);
        }

        // 触发AbstractChannelHandlerContext内的HandlerRemoved
        @Override
        void execute() {
            EventExecutor executor = ctx.executor();
            if (executor.inEventLoop(Thread.currentThread())) {
                callHandlerRemoved0(ctx);
            } else {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(
                                "Can't invoke handlerRemoved() as the EventExecutor {} rejected it," +
                                        " removing handler {}.", executor, ctx.name(), e);
                    }
                    ctx.setRemoved();
                }
            }
        }
    }

}
