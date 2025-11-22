package com.yy.netty.test.unsafe;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * 利用反射工具获取调用者身份demo
 */
public class CallerClassTest {

    public static void main(String[] args) {
        YoungPeople youngPeople = new YoungPeople();
        youngPeople.buyHouse();
    }

    static class Parent {

        // Reflection.getCallerClass() 是一个特殊的 native 方法，它要求：
        // 1.调用它的方法必须被 @CallerSensitive 注解标记
        // 2.这是为了防止滥用，确保只有 JDK 内部或特定授权的代码才能获取调用者信息
        @CallerSensitive
        void buyHouse() {
            // 获取调用者，原理就是获取当前线程的调用栈，进而获取到调用者的类,
            // 当前这个例子中
            // 参数0表示调用方法getCallerClass的当前类（即Reflection），
            // 参数1表示调用方法getCallerClass的堆栈+1的类（即Parent），
            // 参数2表示调用方法getCallerClass的堆栈+2的类（即YoungPeople）
            Class<?> clazz = Reflection.getCallerClass(2);
            System.out.println("父母为" + clazz.getName() + "买房子");

            takeMoney();
        }

        void takeMoney() {
            Class<?> clazz = Reflection.getCallerClass(2);
            System.out.println("父母为" + clazz.getName() + "掏钱");
        }
    }

    static class YoungPeople {
        void buyHouse() {
            System.out.println("年轻人要买房子了");
            Parent parent = new Parent();
            // 让父母帮忙买房子
            parent.buyHouse();
            parent.takeMoney();

            ParentA parentA = new ParentA();
            parentA.buyHouse();
        }
    }

    /**
     * 使用 Thread.currentThread().getStackTrace() 获取调用栈,来实现和 Reflection.getCallerClass()类似的功能
     */
    static class ParentA {
        void buyHouse() {
            // 使用 Thread.currentThread().getStackTrace() 获取调用栈
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            // stackTrace[2] 通常是实际的调用者
            String callerClassName = stackTrace[2].getClassName();
            System.out.println("父母为" + callerClassName + "买房子");
        }
    }

}
