# Netty-ZeroToOne
从零到一手搓netty

## version-01
* 目标：框架异步化初步抽象，初步构建Reactor模型；
* 设计与实现：通过SingleThreadEventExecutor、SingleThreadEventLoop、NioEventLoop三层抽象，实现对事件的独立单线程异步处理；
* 功能与效果：开放了IO事件的注册行为，单线程循环处理IO事件的注册任务+对IO事件的响应处理。最终通过构建不同的NioEventLoop即可处理不同的IO事件。使用案例和效果请参考ServerTest和ClientTest两个测试类。

## version-02
* 目标：增加Group工作组的概念，提升框架并发处理能力；
* 设计与实现：TODO
* 功能与效果：TODO





