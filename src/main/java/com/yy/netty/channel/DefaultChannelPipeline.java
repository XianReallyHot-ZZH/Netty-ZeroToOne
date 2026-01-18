package com.yy.netty.channel;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

/**
 * 默认的ChannelPipeline实现类,
 */
public class DefaultChannelPipeline implements ChannelPipeline {
    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
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
    public ChannelInboundInvoker fireChannelWritabilityChanged() {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireChannelRegistered() {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireChannelUnregistered() {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireChannelActive() {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireExceptionCaught(Throwable cause) {
        return null;
    }

    @Override
    public ChannelInboundInvoker fireUserEventTriggered(Object event) {
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
}
