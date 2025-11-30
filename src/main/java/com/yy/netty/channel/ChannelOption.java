package com.yy.netty.channel;

import com.yy.netty.util.AbstractConstant;
import com.yy.netty.util.ConstantPool;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * channel的参数项，底层是一种常量。
 */
public class ChannelOption<T> extends AbstractConstant<ChannelOption<T>> {

    // ChannelOption参数项的全局ChannelOption常量池
    private static final ConstantPool<ChannelOption<Object>> pool = new ConstantPool<ChannelOption<Object>>() {
        /**
         * 定义实现具体类型的常量对象
         *
         * @param id
         * @param name
         * @return
         */
        @Override
        protected ChannelOption<Object> newConstant(int id, String name) {
            return new ChannelOption<Object>(id, name);
        }
    };

    /**
     * 根据名称获取对应的ChannelOption对象
     *
     * @param name
     * @param <T>
     * @return
     */
    public static <T> ChannelOption<T> valueOf(String name) {
        // TODO：待打断点验证，这里真的能进行类型转化吗？
        return (ChannelOption<T>) pool.valueOf(name);
    }

    public static <T> ChannelOption<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (ChannelOption<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    /**
     * 判断是否存在对应的ChannelOption对象
     *
     * @param name
     * @return
     */
    public static boolean exists(String name) {
        return pool.exists(name);
    }

    // ---------------------------------------------- 下面的这些参数项都是netty作者已经创建好、内置好的常量 --------------------------------------------------
    public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");
    public static final ChannelOption<Integer> WRITE_SPIN_COUNT = valueOf("WRITE_SPIN_COUNT");
    public static final ChannelOption<Boolean> ALLOW_HALF_CLOSURE = valueOf("ALLOW_HALF_CLOSURE");
    public static final ChannelOption<Boolean> AUTO_READ = valueOf("AUTO_READ");
    public static final ChannelOption<Boolean> AUTO_CLOSE = valueOf("AUTO_CLOSE");
    public static final ChannelOption<Boolean> SO_BROADCAST = valueOf("SO_BROADCAST");
    public static final ChannelOption<Boolean> SO_KEEPALIVE = valueOf("SO_KEEPALIVE");
    public static final ChannelOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");
    public static final ChannelOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");
    public static final ChannelOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");
    public static final ChannelOption<Integer> SO_LINGER = valueOf("SO_LINGER");
    /**
     * 记得我们给channel配置的参数吗option(ChannelOption.SO_BACKLOG,128)，是不是很熟悉，我们拿来即用的常量，因为作者
     * 已经创建好了，找找还有你熟悉的名字吗？这里我多说一句，不要被ChannelOption<T>中的泛型给迷惑了，觉得ChannelOption中也存储着
     * 用户定义的值，就是那个泛型的值，比如说option(ChannelOption.SO_BACKLOG,128)里面的128，以为ChannelOption<Integer>中的integer
     * 存储的就是128，实际上128存储在serverbootstrap的linkmap中，ChannelOption不存储参数项的值，本质上ChannelOption只是对参数项的定义。
     * 而作者之所以给常量类设定泛型，是因为Attribut会存储泛型的值，这个待后面实现
     */
    public static final ChannelOption<Integer> SO_BACKLOG = valueOf("SO_BACKLOG");
    public static final ChannelOption<Integer> SO_TIMEOUT = valueOf("SO_TIMEOUT");
    public static final ChannelOption<Integer> IP_TOS = valueOf("IP_TOS");
    public static final ChannelOption<InetAddress> IP_MULTICAST_ADDR = valueOf("IP_MULTICAST_ADDR");
    public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = valueOf("IP_MULTICAST_IF");
    public static final ChannelOption<Integer> IP_MULTICAST_TTL = valueOf("IP_MULTICAST_TTL");
    public static final ChannelOption<Boolean> IP_MULTICAST_LOOP_DISABLED = valueOf("IP_MULTICAST_LOOP_DISABLED");
    public static final ChannelOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");
    public static final ChannelOption<Boolean> SINGLE_EVENTEXECUTOR_PER_GROUP =
            valueOf("SINGLE_EVENTEXECUTOR_PER_GROUP");


    protected ChannelOption(String name) {
        this(pool.nextId(), name);
    }

    private ChannelOption(int id, String name) {
        super(id, name);
    }

    public void validate(T value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
    }

}
