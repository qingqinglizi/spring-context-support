package org.springframework.scheduling.commonj;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.naming.NamingException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

public class WorkManagerTaskExecutor
        extends JndiLocatorSupport
        implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, InitializingBean
{
    private WorkManager workManager;
    private String workManagerName;
    private WorkListener workListener;
    private TaskDecorator taskDecorator;

    public void setWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    public void setWorkManagerName(String workManagerName)
    {
        this.workManagerName = workManagerName;
    }

    public void setWorkListener(WorkListener workListener)
    {
        this.workListener = workListener;
    }

    public void setTaskDecorator(TaskDecorator taskDecorator)
    {
        this.taskDecorator = taskDecorator;
    }

    public void afterPropertiesSet()
            throws NamingException
    {
        if (this.workManager == null)
        {
            if (this.workManagerName == null) {
                throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
            }
            this.workManager = ((WorkManager)lookup(this.workManagerName, WorkManager.class));
        }
    }

    public void execute(Runnable task)
    {
        Assert.state(this.workManager != null, "No WorkManager specified");
        Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
        try
        {
            if (this.workListener != null) {
                this.workManager.schedule(work, this.workListener);
            } else {
                this.workManager.schedule(work);
            }
        }
        catch (WorkRejectedException ex)
        {
            throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
        }
        catch (WorkException ex)
        {
            throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
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

    public WorkItem schedule(Work work)
            throws WorkException, IllegalArgumentException
    {
        return this.workManager.schedule(work);
    }

    public WorkItem schedule(Work work, WorkListener workListener)
            throws WorkException
    {
        return this.workManager.schedule(work, workListener);
    }

    public boolean waitForAll(Collection workItems, long timeout)
            throws InterruptedException
    {
        return this.workManager.waitForAll(workItems, timeout);
    }

    public Collection waitForAny(Collection workItems, long timeout)
            throws InterruptedException
    {
        return this.workManager.waitForAny(workItems, timeout);
    }
}
