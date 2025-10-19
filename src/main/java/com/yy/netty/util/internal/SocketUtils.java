package com.yy.netty.util.internal;

import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class SocketUtils {

    public static InetSocketAddress socketAddress(final String hostname, final int port) {
        // 为了提高代码的兼容性和健壮性,使用AccessController来包装new InetSocketAddress的逻辑，
        // 因为AccessController.doPrivileged可以在安全管理器限制的情况下，临时绕过安全检查，获取到更高的权限
        // 创建 InetSocketAddress这类网络操作，在安全策略中可能被限制，需要特殊权限才能执行，所以进行这一层的包装
        return AccessController.doPrivileged(new PrivilegedAction<InetSocketAddress>() {
            @Override
            public InetSocketAddress run() {
                return new InetSocketAddress(hostname, port);
            }
        });
    }

}
