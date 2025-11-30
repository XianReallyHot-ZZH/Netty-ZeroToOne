package com.yy.netty.util;

/**
 * 常量类的顶级接口，定义了常量的id和名字
 * <p>
 * 这个泛型定义 T extends Constant<T> 是一种递归泛型约束（也称为自我绑定），它用于确保实现该接口的具体类能够与其自身类型保持一致。
 * 意味着 T 必须是 Constant 的某个子类型，并且这个子类型必须是它自己的实例。换句话说，每个实现了 Constant 接口的类都应该是这样的形式：
 * public class MyConstant implements Constant<MyConstant>
 * </p>
 * <p>
 * 通过这种方式定义泛型，可以保证：
 * (1)每个具体实现类只能和其他同类型的实例进行比较；
 * (2)避免不同类型之间的非法比较；
 * (3)提供编译期类型检查，增强程序的安全性和可读性。
 * </p>
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    int id();

    String name();

}
