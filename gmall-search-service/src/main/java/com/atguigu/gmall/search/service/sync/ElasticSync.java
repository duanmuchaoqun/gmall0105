package com.atguigu.gmall.search.service.sync;

/**
 * 多线程同步模型
 */
public class ElasticSync implements Runnable {

    private int index;

    public ElasticSync(int index) {
        this.index = index;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName()+"执行了第"+index+"任务");
    }
}
