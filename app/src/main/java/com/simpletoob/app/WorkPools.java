package com.simpletoob.app;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Bounded queues never run rejected work on the calling/UI thread. */
public final class WorkPools {
    private WorkPools(){}
    public static ThreadPoolExecutor create(String name,int threads,int capacity,Runnable initialize){
        AtomicInteger ids=new AtomicInteger();
        ThreadFactory factory=task->{Thread t=new Thread(()->{initialize.run();task.run();},name+"-"+ids.incrementAndGet());return t;};
        ThreadPoolExecutor pool=new ThreadPoolExecutor(threads,threads,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(capacity),factory,new ThreadPoolExecutor.AbortPolicy()){
            @Override public void execute(Runnable task){purge();super.execute(task);}
        };
        pool.allowCoreThreadTimeOut(true);return pool;
    }
}
