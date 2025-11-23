package com.yy.netty.channel.nio;

import com.yy.netty.channel.Channel;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * 客户端NIO消息Channel抽象类，完成一些客户端消息处理的通用方法，持有一些客户端消息处理需要的成员变量
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {
    /**
     * 构造方法
     *
     * @param parent 服务端产生的客户端SocketChannel才会有父Channel
     * @param ch     对应的NIO Channel
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    protected final class NioByteUnsafe extends AbstractNioUnsafe {

        /**
         * 客户端channel“读”事件处理逻辑:
         * 其实就是从SocketChannel上进行IO读取
         */
        @Override
        public void read() {
            //暂时用最原始简陋的方法处理
            ByteBuffer byteBuf = ByteBuffer.allocate(1024);
            try {
                doReadBytes(byteBuf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 抽象方法，子类具体实现对SocketChannel的IO读取
     *
     * @param byteBuf
     * @throws Exception
     */
    protected abstract int doReadBytes(ByteBuffer byteBuf) throws Exception;
}
