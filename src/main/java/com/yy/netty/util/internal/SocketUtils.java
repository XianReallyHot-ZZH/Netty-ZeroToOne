package com.yy.netty.util.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

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

    /**
     * 客户端socketChannel连接至远程服务器
     *
     * @param socketChannel
     * @param remoteAddress
     * @return
     * @throws IOException
     */
    public static boolean connect(final SocketChannel socketChannel, final SocketAddress remoteAddress)
            throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws IOException {
                    return socketChannel.connect(remoteAddress);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    /**
     * 服务端socketChannel接受客户端连接,生成客户端channel，并返回
     *
     * @param serverSocketChannel
     * @return
     * @throws IOException
     */
    public static SocketChannel accept(final ServerSocketChannel serverSocketChannel) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<SocketChannel>() {
                @Override
                public SocketChannel run() throws IOException {
                    return serverSocketChannel.accept();
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }

    /**
     * 客户端socketChannel绑定到本地端口
     *
     * @param socketChannel
     * @param address
     * @throws IOException
     */
    public static void bind(final SocketChannel socketChannel, final SocketAddress address) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    socketChannel.bind(address);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }


    /**
     * 获取服务端的本地端口
     *
     * @param socket
     * @return
     */
    public static SocketAddress localSocketAddress(ServerSocket socket) {
        return AccessController.doPrivileged(new PrivilegedAction<SocketAddress>() {
            @Override
            public SocketAddress run() {
                return socket.getLocalSocketAddress();
            }
        });
    }
}
