package org.springframework.scheduling.quartz;

import org.quartz.JobDetail;
import org.quartz.SchedulerContext;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

public class SpringBeanJobFactory
        extends AdaptableJobFactory
        implements SchedulerContextAware
{
    private String[] ignoredUnknownProperties;
    private SchedulerContext schedulerContext;

    public void setIgnoredUnknownProperties(String... ignoredUnknownProperties)
    {
        this.ignoredUnknownProperties = ignoredUnknownProperties;
    }

    public void setSchedulerContext(SchedulerContext schedulerContext)
    {
        this.schedulerContext = schedulerContext;
    }

    protected Object createJobInstance(TriggerFiredBundle bundle)
            throws Exception
    {
        Object job = super.createJobInstance(bundle);
        if (isEligibleForPropertyPopulation(job))
        {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
            MutablePropertyValues pvs = new MutablePropertyValues();
            if (this.schedulerContext != null) {
                pvs.addPropertyValues(this.schedulerContext);
            }
            pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
            pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
            if (this.ignoredUnknownProperties != null)
            {
                for (String propName : this.ignoredUnknownProperties) {
                    if ((pvs.contains(propName)) && (!bw.isWritableProperty(propName))) {
                        pvs.removePropertyValue(propName);
                    }
                }
                bw.setPropertyValues(pvs);
            }
            else
            {
                bw.setPropertyValues(pvs, true);
            }
        }
        return job;
    }

    protected boolean isEligibleForPropertyPopulation(Object jobObject)
    {
        return !(jobObject instanceof QuartzJobBean);
    }
}
