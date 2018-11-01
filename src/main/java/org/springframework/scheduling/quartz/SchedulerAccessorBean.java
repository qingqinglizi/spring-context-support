package org.springframework.scheduling.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.SchedulerRepository;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;

public class SchedulerAccessorBean
        extends SchedulerAccessor
        implements BeanFactoryAware, InitializingBean
{
    private String schedulerName;
    private Scheduler scheduler;
    private BeanFactory beanFactory;

    public void setSchedulerName(String schedulerName)
    {
        this.schedulerName = schedulerName;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler()
    {
        return this.scheduler;
    }

    public void setBeanFactory(BeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    public void afterPropertiesSet()
            throws SchedulerException
    {
        if (this.scheduler == null) {
            this.scheduler = (this.schedulerName != null ? findScheduler(this.schedulerName) : findDefaultScheduler());
        }
        registerListeners();
        registerJobsAndTriggers();
    }

    protected Scheduler findScheduler(String schedulerName)
            throws SchedulerException
    {
        if ((this.beanFactory instanceof ListableBeanFactory))
        {
            ListableBeanFactory lbf = (ListableBeanFactory)this.beanFactory;
            String[] beanNames = lbf.getBeanNamesForType(Scheduler.class);
            for (String beanName : beanNames)
            {
                Scheduler schedulerBean = (Scheduler)lbf.getBean(beanName);
                if (schedulerName.equals(schedulerBean.getSchedulerName())) {
                    return schedulerBean;
                }
            }
        }
        Scheduler schedulerInRepo = SchedulerRepository.getInstance().lookup(schedulerName);
        if (schedulerInRepo == null) {
            throw new IllegalStateException("No Scheduler named '" + schedulerName + "' found");
        }
        return schedulerInRepo;
    }

    protected Scheduler findDefaultScheduler()
    {
        if (this.beanFactory != null) {
            return (Scheduler)this.beanFactory.getBean(Scheduler.class);
        }
        throw new IllegalStateException("No Scheduler specified, and cannot find a default Scheduler without a BeanFactory");
    }
}
