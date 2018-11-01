package org.springframework.scheduling.quartz;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

public class LocalTaskExecutorThreadPool
        implements ThreadPool
{
    protected final Log logger = LogFactory.getLog(getClass());
    private Executor taskExecutor;

    public void setInstanceId(String schedInstId) {}

    public void setInstanceName(String schedName) {}

    public void initialize()
            throws SchedulerConfigException
    {
        this.taskExecutor = SchedulerFactoryBean.getConfigTimeTaskExecutor();
        if (this.taskExecutor == null) {
            throw new SchedulerConfigException("No local TaskExecutor found for configuration - 'taskExecutor' property must be set on SchedulerFactoryBean");
        }
    }

    public void shutdown(boolean waitForJobsToComplete) {}

    public int getPoolSize()
    {
        return -1;
    }

    public boolean runInThread(Runnable runnable)
    {
        if (runnable == null) {
            return false;
        }
        try
        {
            this.taskExecutor.execute(runnable);
            return true;
        }
        catch (RejectedExecutionException ex)
        {
            this.logger.error("Task has been rejected by TaskExecutor", ex);
        }
        return false;
    }

    public int blockForAvailableThreads()
    {
        return 1;
    }
}
