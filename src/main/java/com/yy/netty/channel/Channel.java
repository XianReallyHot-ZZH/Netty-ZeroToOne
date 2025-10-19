package com.yy.netty.channel;

/**
 * netty channel的顶级接口，先引入一部分方法好了
 * netty channel体系的目的：
 * 1、增强原生NIO channel的功能
 * 2、进行各层抽象与实现后，达到复用部分代码实现的目的，同时留出给上层自定义的方法
 * 3、适配netty的总体规划
 */
public interface Channel {
}
