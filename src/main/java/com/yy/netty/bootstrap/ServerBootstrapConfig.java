package com.yy.netty.bootstrap;

import com.yy.netty.channel.Channel;
import com.yy.netty.channel.EventLoopGroup;
import com.yy.netty.util.internal.StringUtil;

public class ServerBootstrapConfig extends AbstractBootstrapConfig<ServerBootstrap, Channel> {

    protected ServerBootstrapConfig(ServerBootstrap bootstrap) {
        super(bootstrap);
    }

    public EventLoopGroup childGroup() {
        return bootstrap.childGroup();
    }

    /**
     * 重写toString方法，增加对workGroup的描述信息，在服务端引导类中，这个workGroup还是很重要的
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        EventLoopGroup childGroup = childGroup();
        if (childGroup != null) {
            buf.append("childGroup: ");
            buf.append(StringUtil.simpleClassName(childGroup));
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }
}
