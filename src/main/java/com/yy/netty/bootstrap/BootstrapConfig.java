package com.yy.netty.bootstrap;

import com.yy.netty.channel.Channel;

import java.net.SocketAddress;

public class BootstrapConfig extends AbstractBootstrapConfig<Bootstrap, Channel> {

    BootstrapConfig(Bootstrap bootstrap) {
        super(bootstrap);
    }

    public SocketAddress remoteAddress() {
        return bootstrap.remoteAddress();
    }

    /**
     * 重写toString方法，增加对remoteAddress的描述信息
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", resolver: ");
        SocketAddress remoteAddress = remoteAddress();
        if (remoteAddress != null) {
            buf.append(", remoteAddress: ")
                    .append(remoteAddress);
        }
        return buf.append(')').toString();
    }

}
