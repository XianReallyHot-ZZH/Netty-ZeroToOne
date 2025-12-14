package com.yy.netty.channel;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static com.yy.netty.channel.ChannelOption.*;
import static com.yy.netty.util.internal.ObjectUtil.checkPositive;
import static com.yy.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * 默认的channel配置类,对一些基础的、公共的参数做默认实现
 * <p>
 * ChannelConfig体系的设计上最终会延伸到不同类型的NioChannel上，比如NioServerSocketChannel，NioSocketChannel，
 * 最终对应的就是NioSocketChannelConfig和NioServerSocketChannelConfig，是基于DefaultChannelConfig发展出来的
 * </p>
 */
public class DefaultChannelConfig implements ChannelConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    // 原子更新器，改变是否自动读的值，自动读这个属性很重要;这里用原子更新器是为了线程安全
    private static final AtomicIntegerFieldUpdater<DefaultChannelConfig> AUTOREAD_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(DefaultChannelConfig.class, "autoRead");

    // 持有一个channel,和netty的channel是一对一的关系
    protected final Channel channel;

    // 通用参数：连接超时时间，默认30秒
    private volatile int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT;

    // 通用参数：写自旋次数,默认16
    private volatile int writeSpinCount = 16;

    // 通用参数：是否自动读取，默认为1，表示自动读取，0表示手动读取
    private volatile int autoRead = 1;

    // 通用参数：是否自动关闭，默认为true，表示自动关闭
    private volatile boolean autoClose = true;

    public DefaultChannelConfig(Channel channel) {
        this.channel = channel;
    }


    /**
     * 获取所有默认参数项及其值
     * 子类可以继承这个类，并添加自己支持的参数项及其值
     *
     * @return
     */
    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        // 默认支持如下ChannelOption参数
        return getOptions(null, CONNECT_TIMEOUT_MILLIS, WRITE_SPIN_COUNT,
                AUTO_READ, AUTO_CLOSE, SINGLE_EVENTEXECUTOR_PER_GROUP);
    }

    /**
     * 往集合参数result中添加参数项及其值，并返回集合参数的结果
     *
     * @param result
     * @param options
     * @return
     */
    protected Map<ChannelOption<?>, Object> getOptions(Map<ChannelOption<?>, Object> result, ChannelOption<?>... options) {
        if (result == null) {
            //IdentityHashMap是java自己的map，这个map允许放入相同的key，实际上是因为这个map判断相等采用的是地址值
            //地址值不同的两个对象，即便hash值相等，也可以放入map中
            result = new IdentityHashMap<ChannelOption<?>, Object>();
        }
        for (ChannelOption<?> o : options) {
            result.put(o, getOption(o));
        }
        return result;
    }

    // 批量设置参数项及其值，实现上其实还是一个一个的setOption
    @Override
    public boolean setOptions(Map<ChannelOption<?>, ?> options) {
        if (options == null) {
            throw new NullPointerException("options");
        }
        boolean setAllOptions = true;
        for (Map.Entry<ChannelOption<?>, ?> e : options.entrySet()) {
            if (!setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                setAllOptions = false;
            }
        }

        return setAllOptions;
    }

    /**
     * 获取参数项的值
     * 子类可以继承这个类，并添加自己支持的参数项及其值的获取逻辑
     *
     * @param option
     * @return
     * @param <T>
     */
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        // 以下是四个支持的默认参数项，找到各自对应的参数值
        if (option == CONNECT_TIMEOUT_MILLIS) {
            return (T) Integer.valueOf(getConnectTimeoutMillis());
        }
        if (option == WRITE_SPIN_COUNT) {
            return (T) Integer.valueOf(getWriteSpinCount());
        }
        if (option == AUTO_READ) {
            return (T) Boolean.valueOf(isAutoRead());
        }
        if (option == AUTO_CLOSE) {
            return (T) Boolean.valueOf(isAutoClose());
        }
        return null;
    }

    /**
     * 设置参数项及其值
     * 子类可以继承这个类，并添加自己支持的参数项及其值的设置逻辑
     *
     * @param option
     * @param value
     * @return
     * @param <T>
     */
    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);
        // 目前支持对应着四个成员变量的四个参数项
        if (option == CONNECT_TIMEOUT_MILLIS) {
            setConnectTimeoutMillis((Integer) value);
        } else if (option == WRITE_SPIN_COUNT) {
            setWriteSpinCount((Integer) value);
        } else if (option == AUTO_READ) {
            setAutoRead((Boolean) value);
        } else if (option == AUTO_CLOSE) {
            setAutoClose((Boolean) value);
        } else {
            return false;
        }
        return true;
    }

    // 验证传入的参数项及其值的有效性
    protected <T> void validate(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        option.validate(value);
    }

    @Override
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    @Override
    public ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        checkPositiveOrZero(connectTimeoutMillis, "connectTimeoutMillis");
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    @Override
    public int getWriteSpinCount() {
        return writeSpinCount;
    }

    @Override
    public ChannelConfig setWriteSpinCount(int writeSpinCount) {
        checkPositive(writeSpinCount, "writeSpinCount");
        if (writeSpinCount == Integer.MAX_VALUE) {
            --writeSpinCount;
        }
        this.writeSpinCount = writeSpinCount;
        return this;
    }

    @Override
    public boolean isAutoRead() {
        //默认为true的意思
        return autoRead == 1;
    }

    @Override
    public ChannelConfig setAutoRead(boolean autoRead) {
        // 根据入参设置自动读取的值
        boolean oldAutoRead = AUTOREAD_UPDATER.getAndSet(this, autoRead ? 1 : 0) == 1;
        if (autoRead && !oldAutoRead) {
            // 如果设置为自动读取，并且之前自动读取是关闭的，那么触发一次channel的读取（实际是触发channel的beginRead）
            channel.read();
        } else if (!autoRead && oldAutoRead) {
            // 如果设置为手动读取，并且之前自动读取是打开的，那么触发一次自动读取的清理
            autoReadCleared();
        }
        return this;
    }

    // 默认空实现，子类可能会重写这个方法
    protected void autoReadCleared() {
    }

    @Override
    public boolean isAutoClose() {
        return autoClose;
    }

    @Override
    public ChannelConfig setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
        return this;
    }

    // 这个参数目前还没有，后面看情况补充
    @Override
    public int getWriteBufferHighWaterMark() {
        return 0;
    }

    // 这个参数目前还没有，后面看情况补充
    @Override
    public ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        return null;
    }

    // 这个参数目前还没有，后面看情况补充
    @Override
    public int getWriteBufferLowWaterMark() {
        return 0;
    }

    // 这个参数目前还没有，后面看情况补充
    @Override
    public ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        return null;
    }
}
