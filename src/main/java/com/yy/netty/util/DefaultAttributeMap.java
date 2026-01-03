package com.yy.netty.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * AttributeMap的实现类，AbstractChannel后续会继承该实现类，进而赋予channel map容器的角色，那么channel其实就可以当做容器来存储数据了
 * <p>
 * 整套实现，其实参考的是JDK的HashMap，也是用数组+链表来构造桶这种结构来存储数据的，存储的数据对象其实就是Attribute对象
 * 解释一下：为什么netty要自己实现一个AttributeMap呢？直接继承JDK的Map实现类不就好了吗？
 * 答案是netty作者觉得JDK的HashMap解决不了并发问题，ConcurrentHashMap的性能又觉得不行，所以就直接自己实现一个了。
 *
 */
public class DefaultAttributeMap implements AttributeMap {

    //数组的初始大小为4
    private static final int BUCKET_SIZE = 4;
    //掩码为3，要做位运算求数组下标，这意味着该数组不必扩容
    private static final int MASK = BUCKET_SIZE - 1;
    // 原子更新器，这个更新器更新的是哈希桶数组attributes对象，解决的是attributes初始化设置的并发问题
    private static final AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> updater =
            AtomicReferenceFieldUpdater.newUpdater(DefaultAttributeMap.class, AtomicReferenceArray.class, "attributes");

    //哈希桶数组，并不在这里初始化。这里使用AtomicReferenceArray，是为了解决同一个索引下标元素设置的并发问题
    private volatile AtomicReferenceArray<DefaultAttribute<?>> attributes;


    /**
     * 该方法的作用是获取AttributeKey对应的Attribute对象，如果存在，那么直接返回，否则创建一个Attribute对象，并返回
     *
     * @param key
     * @param <T>
     * @return
     */
    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        AtomicReferenceArray<DefaultAttribute<?>> attributes = this.attributes;
        //如果数组不等于null，说明已经初始化过，不是第一次向map中存数据了,那么跳过下面的if逻辑
        if (attributes == null) {
            //为null则初始化，数组的长度是固定的
            attributes = new AtomicReferenceArray<DefaultAttribute<?>>(BUCKET_SIZE);
            //用原子更新器把attributes更新成初始化好的数组，这里要注意一下，虽然上面数组已经初始化好了，但初始化好的数组赋值给了局部变量
            //到这里，才真正把初始化好的数组给到了对象中的attributes属性
            if (!updater.compareAndSet(this, null, attributes)) {
                // 被其他线程设置成功，那么将设置成功的数组赋值给局部变量attributes
                attributes = this.attributes;
            }
        }
        //计算数据在数组中存储的下标
        int i = index(key);
        //这里就可以类比向hashmap中添加数据的过程了，计算出下标后，先判断该下标上是否有数据
        DefaultAttribute<?> head = attributes.get(i);
        //为null则说明暂时没有数据，可以直接添加，否则就要以链表的形式添加。这里当然也不能忘记并发的情况，如果多个线程都洗向这个位置添加数据呢
        if (head == null) {
            //初始化一个头节点，但里面不存储任何数据
            head = new DefaultAttribute();
            //创建一个DefaultAttribute对象，把头节点和key作为参数传进去。实际上，这里创建的DefaultAttribute对象就是该map中存储的value对象
            //当然，这么说也不准确，确切地说，应该是要存储的value就存放在DefaultAttribute对象中，而DefaultAttribute对象存储在数组中
            DefaultAttribute<T> attr = new DefaultAttribute<T>(head, key);
            // 进行链表节点的添加，把节点之间的关系连起来
            // 头节点是空节点，也是存放在数组的节点，头节点的下一个节点就是刚刚创建的attr对象
            head.next = attr;
            //att的前一个节点就是头节点
            attr.prev = head;
            // 这里有并发问题，所以用cas给数组下标位置赋值，只有一个线程原子添加会成功
            if (attributes.compareAndSet(i, null, head)) {
                //返回创建的DefaultAttribute对象
                return attr;
            } else {
                //走着这里说明该线程设置头节点失败，这时候就要把头节点重新赋值，因为其他线程已经把头节点添加进去了，就要用添加进去的头节点赋值
                head = attributes.get(i);
            }
        }
        //走到这里说明头节点已经初始化过了，说明要添加的位置已经有头节点了，需要以链表的方法继续添加数据或者查找数据
        synchronized (head) {
            //把当前节点赋值为头节点
            DefaultAttribute<?> curr = head;
            for (; ; ) {
                //得到当前节点的下一个节点
                DefaultAttribute<?> next = curr.next;
                //如果为null，说明当前节点就是最后一个节点
                if (next == null) {
                    //创建DefaultAttribute对象，封装数据
                    DefaultAttribute<T> attr = new DefaultAttribute<T>(head, key);
                    // 往链表的尾部添加数据
                    //当前节点下一个节点为attr
                    curr.next = attr;
                    //attr的上一个节点为当前节点，从这里可以看出netty定义的map中链表采用的是尾插法
                    attr.prev = curr;
                    return attr;
                }
                //如果下一个节点和传入的key相等，并且该节点并没有被删除，说明map中已经存在该数据了，直接返回该数据即可
                if (next.key == key && !next.removed) {
                    return (Attribute<T>) next;
                }
                //把下一个节点赋值为当前节点，进入下一个循环
                curr = next;
            }
        }
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        AtomicReferenceArray<DefaultAttribute<?>> attributes = this.attributes;
        // 如果数组为null，说明还没有初始化过，返回false
        if (attributes == null) {
            return false;
        }
        int i = index(key);
        DefaultAttribute<?> head = attributes.get(i);
        // 如果头节点为null，说明还没有数据，返回false
        if (head == null) {
            return false;
        }
        // 这里有上锁的必要吗？安全起见，这里加锁，相当于在读取期间，链表数据不会被其他线程修改，读出来的数据是安全的，但性能会下降
        synchronized (head) {
            DefaultAttribute<?> curr = head.next;
            while (curr != null) {
                if (curr.key == key && !curr.removed) {
                    return true;
                }
                curr = curr.next;
            }
            return false;
        }
    }

    private static int index(AttributeKey<?> key) {
        // 与掩码&运算，数值肯定<=mask 正好是数组下标
        return key.id() & MASK;
    }

    /**
     * 静态内部类，该类继承了AtomicReference，AtomicReference类中存储的才是map数据中真正的value
     * 很快我们就会在别的地方看到这样一行代码channel.attr(key).set(e.getValue());
     * 其中set方法，就是调用了AtomicReference类中的set方法，把要存储的value以cas的方式存储到AtomicReference类中
     * <p>
     * 本身DefaultAttribute还是一个链表的节点，所以内部实现你会看到一些前序节点，后序节点等等这些和链表这种数据结构强相关的东西。
     *
     * @param <T>
     */
    private static final class DefaultAttribute<T> extends AtomicReference<T> implements Attribute<T> {

        private static final long serialVersionUID = -2661411462200283011L;

        // 链表的头结点
        private final DefaultAttribute<?> head;
        // map存储对象Attribute的 key
        private final AttributeKey<T> key;
        // 前节点
        private DefaultAttribute<?> prev;
        // 后节点
        private DefaultAttribute<?> next;

        // 删除标志位
        private volatile boolean removed;

        DefaultAttribute(DefaultAttribute<?> head, AttributeKey<T> key) {
            this.head = head;
            this.key = key;
        }

        // 一般用来创建没有业务含义的链表的head节点
        DefaultAttribute() {
            head = this;
            key = null;
        }


        @Override
        public AttributeKey<T> key() {
            return key;
        }

        /**
         * 如果不存在value，那么就设置value，并返回null
         * 如果存在value，那么就返回已存在的旧值
         *
         * @param value
         * @return
         */
        @Override
        public T setIfAbsent(T value) {
            //原子引用类用cas把要存储的value存储到类中，并发控制
            while (!compareAndSet(null, value)) {
                // value已存在，那么就返回已存在的旧值
                T old = get();
                if (old != null) {
                    return old;
                }
            }
            // value本来是不存在的，那么就返回null
            return null;
        }

        @Override
        public T getAndRemove() {
            removed = true;
            T oldValue = getAndSet(null);
            remove0();
            return oldValue;
        }

        @Override
        public void remove() {
            //表示节点已删除
            removed = true;
            //既然DefaultAttribute都删除了，那么DefaultAttribute中存储的value也该置为null了
            set(null);
            //删除一个节点，重排链表指针
            remove0();
        }

        /**
         * 在链表中删除自身节点
         */
        private void remove0() {
            // 链表结构的调整是会出现并发问题的，所以这里加了锁，后续任何涉及链表结构变化的操作，都会用到这个head锁，用来排他并发
            synchronized (head) {
                if (prev == null) {
                    return;
                }
                // 删除自身节点,将前节点和后节点连接起来
                prev.next = next;
                if (next != null) {
                    next.prev = prev;
                }
                prev = null;
                next = null;
            }
        }
    }

}
