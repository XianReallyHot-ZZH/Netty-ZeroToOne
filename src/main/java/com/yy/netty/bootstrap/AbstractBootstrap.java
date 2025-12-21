package com.yy.netty.bootstrap;

import com.yy.netty.channel.*;
import com.yy.netty.util.concurrent.EventExecutor;
import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.SocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 再次引入抽象类，该类实现了一些bootstrao和serverbootstrap通用的方法
 *
 * @param <B>
 * @param <C>
 */
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBootstrap.class);

    // 服务端 boss事件循环组,负责处理IO accept事件（连接事件）, 也可以是客户端事件循环组，由子类决定
    volatile EventLoopGroup group;

    // channel工厂，用于后续生产指定类型的channel实例
    private volatile ChannelFactory<? extends C> channelFactory;

    private volatile SocketAddress localAddress;

    /**
     * 引导类存储用户设置的参数
     * 1、用户设定的NioServerSocketChannel的参数会暂时存放在这个map中，channel初始化的时候，这里面的数据才会存放到channel的配置类中
     * 2、当然，当你创建的是NioSocketChannel的时候，这里存储的就是与NioSocketChannel有关的参数
     */
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();

    AbstractBootstrap() {

    }

    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
    }

    /**
     * 设置事件循环组
     *
     * @param group
     * @return
     */
    public B group(EventLoopGroup group) {
        ObjectUtil.checkNotNull(group, "group");
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return self();
    }

    private B self() {
        return (B) this;
    }

    /**
     * 设置channel类型
     *
     * @param channelClass
     * @return
     */
    public B channel(Class<? extends C> channelClass) {
        return channelFactory(new ReflectiveChannelFactory<C>(
                ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    /**
     * 设置channel工厂
     *
     * @param channelFactory
     * @return
     */
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        ObjectUtil.checkNotNull(channelFactory, "channelFactory");
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }
        this.channelFactory = channelFactory;
        return self();
    }

    /**
     * 设置本地地址
     *
     * @param localAddress
     * @return
     */
    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return self();
    }

    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }

    public B localAddress(String inetHost, int inetPort) {
        return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
    }

    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * 设置channel参数, 这里面存放的是用户设置的参数，这些参数会临时存放在引导类的options中，等channel初始化的时候，会读取引导类中存储的option参数然后存放到channel的配置类中
     *
     * @param option
     * @param value
     * @param <T>
     * @return
     */
    public <T> B option(ChannelOption<T> option, T value) {
        ObjectUtil.checkNotNull(option, "option");
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return self();
    }

    /**
     * 验证引导类参数
     * 1、检查group是否设置
     * 2、检查channelFactory是否设置
     *
     * @return
     */
    public B validate() {
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
        if (channelFactory == null) {
            throw new IllegalStateException("channel or channelFactory not set");
        }
        return self();
    }

    // 获取group
    public final EventLoopGroup group() {
        return group;
    }

    /**
     * 获取配置类,由子类来决定创建哪个BootstrapConfig引导配置类
     * 1、比如服务端引导类，ServerBootstrapConfig
     * 2、比如客户端引导类，BootstrapConfig
     *
     * @return
     */
    public abstract AbstractBootstrapConfig<B, C> config();

    // 获取options
    final Map<ChannelOption<?>, Object> options0() {
        return options;
    }

    // 获取localAddress
    final SocketAddress localAddress() {
        return localAddress;
    }

    // 获取channelFactory
    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }


    /**
     * 将channel注册到单线程执行器上的方法
     *
     * @return
     */
    public ChannelFuture register() {
        validate();
        return initAndRegister();
    }

    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            //在这里初始化服务端channel，反射创建对象调用的无参构造器
            channel = channelFactory.newChannel();
            //初始化channel
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                //出现异常则强制关闭channel
                channel.unsafe().closeForcibly();
                //返回一个赋值为失败的future
                return new DefaultChannelPromise(channel, channel.eventLoop()).setFailure(t);
            }
        }
        //在这里把channel注册到boss线程组的执行器上
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            //出现异常，但是注册成功了，则直接关闭channel，该方法还未实现，等后面，开发到优雅停机和释放资源时，会填充close方法
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }
        return regFuture;
    }

    /**
     * 由子类来实现，初始化channel的方法，这里定义为抽象的，意味着客户端channel和服务端channel实现的方法各不相同
     *
     * @param channel
     * @throws Exception
     */
    abstract void init(Channel channel) throws Exception;


    /**
     * 将channel绑定到本地地址的方法
     *
     * @return
     */
    public ChannelFuture bind() {
        validate();
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }
        return doBind(localAddress);
    }

    // 一般调用的是这个方法
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }


    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }


    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 1、完成对指定channel的创建，这里其实就是服务端的channel了，然后将其注册到boss组中的单线程执行器的selector上（不带任务感兴趣事件的注册行为，其实就是为了将channel和一个EventLoop进行绑定），
        // 这里还没法成功注册accep事件，因为还没进行端口绑定，在下面绑定端口的逻辑里会真正为channel注册accept事件
        final ChannelFuture regFuture = initAndRegister();
        // 2、得到创建的channel
        Channel channel = regFuture.channel();
        // 初始化绑定阶段出错了，那么直接返回
        if (regFuture.cause() != null) {
            return regFuture;
        }

        // 3、判断channel是否注册完成
        if (regFuture.isDone()) {
            // 服务端channel成功注册到EventLoop了，那么在本线程就可以继续为其绑定本地的服务端口了
            // 绑定的行为是异步的，所以创建一个ChannelFuture，用于协调调用方的线程
            DefaultChannelPromise promise = new DefaultChannelPromise(channel);
            // 执行异步绑定，只有异步绑定成功后，本服务端channel才会真正完成accept事件注册，此时服务channel才真正能接受客户端的连接了
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // 服务端channel还没有成功注册感兴趣的事件，那么绑定本地的服务端口的逻辑就需要挂载到regFuture的监听器上面，channel注册结束了，回调绑定端口的逻辑
            // 为了协助判断服务端channel是否注册成功，使用在该类定义的PendingRegistrationPromise，在DefaultChannelPromise的基础上添加注册是否成功的信息记录
            PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            // 在注册future上添加监听器
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // 说明channel注册失败了
                        promise.setFailure(cause);
                    } else {
                        // 说明channel注册成功，那么开始绑定本地的服务端口了
                        promise.registered();
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    /**
     * 异步绑定服务端端口实现，这里才是真正绑定服务端channel到端口号的方法
     *
     * @param regFuture
     * @param channel
     * @param localAddress
     * @param promise
     */
    private static void doBind0(final ChannelFuture regFuture, final Channel channel, final SocketAddress localAddress, final ChannelPromise promise) {
        // 异步绑定
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //在这里仍要判断一次服务端的channel是否注册成功
                if (regFuture.isSuccess()) {
                    //注册成功之后开始绑定
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    //走到这里说明没有注册成功，把异常赋值给promise
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    /**
     * 工具方法：给channel设置option参数
     *
     * @param channel
     * @param options
     */
    static void setChannelOptions(Channel channel, Map<ChannelOption<?>, Object> options) {
        for (Map.Entry<ChannelOption<?>, Object> e : options.entrySet()) {
            setChannelOption(channel, e.getKey(), e.getValue());
        }
    }

    /**
     * 工具方法：给channel设置option参数
     *
     * @param channel
     * @param options
     */
    static void setChannelOptions(
            Channel channel, Map.Entry<ChannelOption<?>, Object>[] options) {
        for (Map.Entry<ChannelOption<?>, Object> e : options) {
            setChannelOption(channel, e.getKey(), e.getValue());
        }
    }

    /**
     * 工具方法：给channel设置option参数
     *
     * @param channel
     * @param option
     * @param value
     */
    private static void setChannelOption(Channel channel, ChannelOption<?> option, Object value) {
        try {
            if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
                logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
            }
        } catch (Throwable t) {
            logger.warn("Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
        }
    }

    /**
     * 为了增加体现注册是否成功的信息，在DefaultChannelPromise的基础上添加注册是否成功的信息记录
     */
    static class PendingRegistrationPromise extends DefaultChannelPromise {

        private volatile boolean registered;

        public PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        //该方法是该静态类独有的,用于改变成员变量registered的值，该方法被调用的时候，registered赋值为true
        void registered() {
            registered = true;
        }

        @Override
        protected EventExecutor executor() {
            return super.executor();
        }

    }

}
