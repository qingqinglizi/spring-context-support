package org.springframework.scheduling.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public abstract class SchedulerAccessor
        implements ResourceLoaderAware
{
    protected final Log logger = LogFactory.getLog(getClass());
    private boolean overwriteExistingJobs = false;
    private String[] jobSchedulingDataLocations;
    private List<JobDetail> jobDetails;
    private Map<String, Calendar> calendars;
    private List<Trigger> triggers;
    private SchedulerListener[] schedulerListeners;
    private JobListener[] globalJobListeners;
    private TriggerListener[] globalTriggerListeners;
    private PlatformTransactionManager transactionManager;
    protected ResourceLoader resourceLoader;

    public void setOverwriteExistingJobs(boolean overwriteExistingJobs)
    {
        this.overwriteExistingJobs = overwriteExistingJobs;
    }

    public void setJobSchedulingDataLocation(String jobSchedulingDataLocation)
    {
        this.jobSchedulingDataLocations = new String[] { jobSchedulingDataLocation };
    }

    public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations)
    {
        this.jobSchedulingDataLocations = jobSchedulingDataLocations;
    }

    public void setJobDetails(JobDetail... jobDetails)
    {
        this.jobDetails = new ArrayList(Arrays.asList(jobDetails));
    }

    public void setCalendars(Map<String, Calendar> calendars)
    {
        this.calendars = calendars;
    }

    public void setTriggers(Trigger... triggers)
    {
        this.triggers = Arrays.asList(triggers);
    }

    public void setSchedulerListeners(SchedulerListener... schedulerListeners)
    {
        this.schedulerListeners = schedulerListeners;
    }

    public void setGlobalJobListeners(JobListener... globalJobListeners)
    {
        this.globalJobListeners = globalJobListeners;
    }

    public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners)
    {
        this.globalTriggerListeners = globalTriggerListeners;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    public void setResourceLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    protected void registerJobsAndTriggers()
            throws SchedulerException
    {
        TransactionStatus transactionStatus = null;
        if (this.transactionManager != null) {
            transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        }
        try
        {
            if (this.jobSchedulingDataLocations != null)
            {
                clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
                clh.initialize();
                XMLSchedulingDataProcessor dataProcessor = new XMLSchedulingDataProcessor(clh);
                for (String location : this.jobSchedulingDataLocations) {
                    dataProcessor.processFileAndScheduleJobs(location, getScheduler());
                }
            }
            if (this.jobDetails != null) {
                for (JobDetail jobDetail : this.jobDetails) {
                    addJobToScheduler(jobDetail);
                }
            } else {
                this.jobDetails = new LinkedList();
            }
            if (this.calendars != null) {
                for (String calendarName : this.calendars.keySet())
                {
                    Calendar calendar = (Calendar)this.calendars.get(calendarName);
                    getScheduler().addCalendar(calendarName, calendar, true, true);
                }
            }
            if (this.triggers != null) {
                for (Trigger trigger : this.triggers) {
                    addTriggerToScheduler(trigger);
                }
            }
        }
        catch (Throwable ex)
        {
            ClassLoadHelper clh;
            if (transactionStatus != null) {
                try
                {
                    this.transactionManager.rollback(transactionStatus);
                }
                catch (TransactionException tex)
                {
                    this.logger.error("Job registration exception overridden by rollback exception", ex);
                    throw tex;
                }
            }
            if ((ex instanceof SchedulerException)) {
                throw ((SchedulerException)ex);
            }
            if ((ex instanceof Exception)) {
                throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
            }
            throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
        }
        if (transactionStatus != null) {
            this.transactionManager.commit(transactionStatus);
        }
    }

    private boolean addJobToScheduler(JobDetail jobDetail)
            throws SchedulerException
    {
        if ((this.overwriteExistingJobs) || (getScheduler().getJobDetail(jobDetail.getKey()) == null))
        {
            getScheduler().addJob(jobDetail, true);
            return true;
        }
        return false;
    }

    private boolean addTriggerToScheduler(Trigger trigger)
            throws SchedulerException
    {
        boolean triggerExists = getScheduler().getTrigger(trigger.getKey()) != null;
        if ((triggerExists) && (!this.overwriteExistingJobs)) {
            return false;
        }
        JobDetail jobDetail = (JobDetail)trigger.getJobDataMap().remove("jobDetail");
        if (triggerExists)
        {
            if ((jobDetail != null) && (!this.jobDetails.contains(jobDetail)) && (addJobToScheduler(jobDetail))) {
                this.jobDetails.add(jobDetail);
            }
            getScheduler().rescheduleJob(trigger.getKey(), trigger);
        }
        else
        {
            try
            {
                if ((jobDetail != null) && (!this.jobDetails.contains(jobDetail)) && ((this.overwriteExistingJobs) ||
                        (getScheduler().getJobDetail(jobDetail.getKey()) == null)))
                {
                    getScheduler().scheduleJob(jobDetail, trigger);
                    this.jobDetails.add(jobDetail);
                }
                else
                {
                    getScheduler().scheduleJob(trigger);
                }
            }
            catch (ObjectAlreadyExistsException ex)
            {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Unexpectedly found existing trigger, assumably due to cluster race condition: " + ex
                            .getMessage() + " - can safely be ignored");
                }
                if (this.overwriteExistingJobs) {
                    getScheduler().rescheduleJob(trigger.getKey(), trigger);
                }
            }
        }
        return true;
    }

    protected void registerListeners()
            throws SchedulerException
    {
        ListenerManager listenerManager = getScheduler().getListenerManager();
        if (this.schedulerListeners != null) {
            for (SchedulerListener listener : this.schedulerListeners) {
                listenerManager.addSchedulerListener(listener);
            }
        }
        if (this.globalJobListeners != null) {
            for (JobListener listener : this.globalJobListeners) {
                listenerManager.addJobListener(listener);
            }
        }
        if (this.globalTriggerListeners != null) {
            for (TriggerListener listener : this.globalTriggerListeners) {
                listenerManager.addTriggerListener(listener);
            }
        }
    }

    protected abstract Scheduler getScheduler();
}
