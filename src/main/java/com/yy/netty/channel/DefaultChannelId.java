package com.yy.netty.channel;

/**
 * 默认的ChannelId实现，保证ChannelId的唯一性
 * 源码里这个类中调用了很多操作系统方法，我就不引进这个了（不重要哈），引入这个类就需要引入更多不必要的类。所以我直接用最简单的方式代替吧，就搞一个时间戳作为channelId。
 */
public class DefaultChannelId implements ChannelId {

    private static final long serialVersionUID = 3884076183504074063L;

    // channel Id
    private String longValue;

    /**
     * 创建一个channelId对象
     *
     * @return
     */
    public static DefaultChannelId newInstance() {
        return new DefaultChannelId();
    }

    private DefaultChannelId() {
        // 用时间戳来简单表达channelId的唯一性，这里简单处理了，不重要
        long currentTimeMillis = System.currentTimeMillis();
        this.longValue = String.valueOf(currentTimeMillis);
    }

    @Override
    public String asShortText() {
        return null;
    }

    @Override
    public String asLongText() {
        String longValue = this.longValue;
        if (longValue == null) {
            this.longValue = longValue =String.valueOf(System.currentTimeMillis());
        }
        return longValue;
    }

    @Override
    public int compareTo(ChannelId o) {
        return 0;
    }
}
