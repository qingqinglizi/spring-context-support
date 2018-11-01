package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class AdaptableJobFactory
        implements JobFactory
{
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
            throws SchedulerException
    {
        try
        {
            Object jobObject = createJobInstance(bundle);
            return adaptJob(jobObject);
        }
        catch (Exception ex)
        {
            throw new SchedulerException("Job instantiation failed", ex);
        }
    }

    protected Object createJobInstance(TriggerFiredBundle bundle)
            throws Exception
    {
        return bundle.getJobDetail().getJobClass().newInstance();
    }

    protected Job adaptJob(Object jobObject)
            throws Exception
    {
        if ((jobObject instanceof Job)) {
            return (Job)jobObject;
        }
        if ((jobObject instanceof Runnable)) {
            return new DelegatingJob((Runnable)jobObject);
        }
        throw new IllegalArgumentException("Unable to execute job class [" + jobObject.getClass().getName() + "]: only [org.quartz.Job] and [java.lang.Runnable] supported.");
    }
}
