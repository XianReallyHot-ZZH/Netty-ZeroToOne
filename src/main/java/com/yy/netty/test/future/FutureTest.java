package com.yy.netty.test.future;

import jdk.nashorn.internal.ir.CallNode;

import java.util.concurrent.*;
import java.util.concurrent.Future;

public class FutureTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

//        demo1();

        demo2();

    }

    /**
     * FutureTask在线程池中执行，获取结果
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static void demo1() throws ExecutionException, InterruptedException, TimeoutException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(5000);
                return 9090;
            }
        };

        FutureTask<Integer> futureTask = new FutureTask<>(callable);

        // 配合线程池使用
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Future<?> otherFuture = threadPool.submit(futureTask);

        try {
            System.out.println("otherFuture with time res:" + otherFuture.get(1000, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            System.out.println("otherFuture with time out");    // 预期会走到这里
        }

        try {
            System.out.println("futureTask with time res:" + futureTask.get(1000, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            System.out.println("futureTask with time out");     // 预期会走到这里
        }

        //无超时获取结果
        System.out.println("otherFuture res:" + otherFuture.get());     // 预期输出：otherFuture res:null
        System.out.println("futureTask res:" + futureTask.get());       // 预期输出：futureTask res:9090

    }



    private static void demo2() throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("callable start");
                Thread.sleep(5000);
                System.out.println("callable end");
                return 9090;
            }
        };

        FutureTask<Integer> future = new FutureTask<Integer>(callable);

        // 独立线程
        Thread t = new Thread(future);
        // 直接设置结果
//        future.set(1000);
        // 再启动线程
        t.start();      // 如果设置了结果，那么启动线程后，任务没有机会执行了，如果没有设置结果，那么启动线程后，任务会执行，并且会返回结果
        // 当前线程睡一下
        Thread.sleep(1000);
        // 尝试取消任务
//        boolean cancel = future.cancel(false);
//        System.out.println("cancel res: " + cancel); // 如果设置了结果，预期输出：cancel res: false， 如果没有设置结果，预期输出：cancel res: true

        try {
            // 如果设置了结果，那么正常输出
            // 如果没有设置结果，但是被取消了，那么会抛出取消异常
            // 如果没有设置结果，并且没有被取消，那么会阻塞，最终超时异常
            System.out.println( future.get(500, TimeUnit.MILLISECONDS)+"==================main函数");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
