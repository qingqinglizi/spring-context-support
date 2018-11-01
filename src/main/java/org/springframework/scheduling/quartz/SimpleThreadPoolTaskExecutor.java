package org.springframework.scheduling.quartz;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.quartz.SchedulerConfigException;
import org.quartz.simpl.SimpleThreadPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

public class SimpleThreadPoolTaskExecutor
        extends SimpleThreadPool
        implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, InitializingBean, DisposableBean
{
    private boolean waitForJobsToCompleteOnShutdown = false;

    public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown)
    {
        this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
    }

    public void afterPropertiesSet()
            throws SchedulerConfigException
    {
        initialize();
    }

    public void execute(Runnable task)
    {
        Assert.notNull(task, "Runnable must not be null");
        if (!runInThread(task)) {
            throw new SchedulingException("Quartz SimpleThreadPool already shut down");
        }
    }

    public void execute(Runnable task, long startTimeout)
    {
        execute(task);
    }

    public Future<?> submit(Runnable task)
    {
        FutureTask<Object> future = new FutureTask(task, null);
        execute(future);
        return future;
    }

    public <T> Future<T> submit(Callable<T> task)
    {
        FutureTask<T> future = new FutureTask(task);
        execute(future);
        return future;
    }

    public ListenableFuture<?> submitListenable(Runnable task)
    {
        ListenableFutureTask<Object> future = new ListenableFutureTask(task, null);
        execute(future);
        return future;
    }

    public <T> ListenableFuture<T> submitListenable(Callable<T> task)
    {
        ListenableFutureTask<T> future = new ListenableFutureTask(task);
        execute(future);
        return future;
    }

    public boolean prefersShortLivedTasks()
    {
        return true;
    }

    public void destroy()
    {
        shutdown(this.waitForJobsToCompleteOnShutdown);
    }
}
