package org.springframework.scheduling.quartz;

import java.util.Map;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JobDetailFactoryBean
        implements FactoryBean<JobDetail>, BeanNameAware, ApplicationContextAware, InitializingBean
{
    private String name;
    private String group;
    private Class<?> jobClass;
    private JobDataMap jobDataMap = new JobDataMap();
    private boolean durability = false;
    private boolean requestsRecovery = false;
    private String description;
    private String beanName;
    private ApplicationContext applicationContext;
    private String applicationContextJobDataKey;
    private JobDetail jobDetail;

    public void setName(String name)
    {
        this.name = name;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public void setJobClass(Class<?> jobClass)
    {
        this.jobClass = jobClass;
    }

    public void setJobDataMap(JobDataMap jobDataMap)
    {
        this.jobDataMap = jobDataMap;
    }

    public JobDataMap getJobDataMap()
    {
        return this.jobDataMap;
    }

    public void setJobDataAsMap(Map<String, ?> jobDataAsMap)
    {
        getJobDataMap().putAll(jobDataAsMap);
    }

    public void setDurability(boolean durability)
    {
        this.durability = durability;
    }

    public void setRequestsRecovery(boolean requestsRecovery)
    {
        this.requestsRecovery = requestsRecovery;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    public void setApplicationContext(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public void setApplicationContextJobDataKey(String applicationContextJobDataKey)
    {
        this.applicationContextJobDataKey = applicationContextJobDataKey;
    }

    public void afterPropertiesSet()
    {
        if (this.name == null) {
            this.name = this.beanName;
        }
        if (this.group == null) {
            this.group = "DEFAULT";
        }
        if (this.applicationContextJobDataKey != null)
        {
            if (this.applicationContext == null) {
                throw new IllegalStateException("JobDetailBean needs to be set up in an ApplicationContext to be able to handle an 'applicationContextJobDataKey'");
            }
            getJobDataMap().put(this.applicationContextJobDataKey, this.applicationContext);
        }
        JobDetailImpl jdi = new JobDetailImpl();
        jdi.setName(this.name);
        jdi.setGroup(this.group);
        jdi.setJobClass(this.jobClass);
        jdi.setJobDataMap(this.jobDataMap);
        jdi.setDurability(this.durability);
        jdi.setRequestsRecovery(this.requestsRecovery);
        jdi.setDescription(this.description);
        this.jobDetail = jdi;
    }

    public JobDetail getObject()
    {
        return this.jobDetail;
    }

    public Class<?> getObjectType()
    {
        return JobDetail.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
