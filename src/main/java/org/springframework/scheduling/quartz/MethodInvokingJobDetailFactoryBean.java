package org.springframework.scheduling.quartz;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

public class MethodInvokingJobDetailFactoryBean
        extends ArgumentConvertingMethodInvoker
        implements FactoryBean<JobDetail>, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, InitializingBean
{
    private String name;
    private String group = "DEFAULT";
    private boolean concurrent = true;
    private String targetBeanName;
    private String beanName;
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
    private BeanFactory beanFactory;
    private JobDetail jobDetail;

    public void setName(String name)
    {
        this.name = name;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public void setConcurrent(boolean concurrent)
    {
        this.concurrent = concurrent;
    }

    public void setTargetBeanName(String targetBeanName)
    {
        this.targetBeanName = targetBeanName;
    }

    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    public void setBeanClassLoader(ClassLoader classLoader)
    {
        this.beanClassLoader = classLoader;
    }

    public void setBeanFactory(BeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    protected Class<?> resolveClassName(String className)
            throws ClassNotFoundException
    {
        return ClassUtils.forName(className, this.beanClassLoader);
    }

    public void afterPropertiesSet()
            throws ClassNotFoundException, NoSuchMethodException
    {
        prepare();


        String name = this.name != null ? this.name : this.beanName;


        Class<?> jobClass = this.concurrent ? MethodInvokingJob.class : StatefulMethodInvokingJob.class;


        JobDetailImpl jdi = new JobDetailImpl();
        jdi.setName(name);
        jdi.setGroup(this.group);
        jdi.setJobClass(jobClass);
        jdi.setDurability(true);
        jdi.getJobDataMap().put("methodInvoker", this);
        this.jobDetail = jdi;

        postProcessJobDetail(this.jobDetail);
    }

    protected void postProcessJobDetail(JobDetail jobDetail) {}

    public Class<?> getTargetClass()
    {
        Class<?> targetClass = super.getTargetClass();
        if ((targetClass == null) && (this.targetBeanName != null))
        {
            Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
            targetClass = this.beanFactory.getType(this.targetBeanName);
        }
        return targetClass;
    }

    public Object getTargetObject()
    {
        Object targetObject = super.getTargetObject();
        if ((targetObject == null) && (this.targetBeanName != null))
        {
            Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
            targetObject = this.beanFactory.getBean(this.targetBeanName);
        }
        return targetObject;
    }

    public JobDetail getObject()
    {
        return this.jobDetail;
    }

    public Class<? extends JobDetail> getObjectType()
    {
        return this.jobDetail != null ? this.jobDetail.getClass() : JobDetail.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    @PersistJobDataAfterExecution
    @DisallowConcurrentExecution
    public static class StatefulMethodInvokingJob
            extends MethodInvokingJobDetailFactoryBean.MethodInvokingJob
    {}

    public static class MethodInvokingJob
            extends QuartzJobBean
    {
        protected static final Log logger = LogFactory.getLog(MethodInvokingJob.class);
        private MethodInvoker methodInvoker;

        public void setMethodInvoker(MethodInvoker methodInvoker)
        {
            this.methodInvoker = methodInvoker;
        }

        protected void executeInternal(JobExecutionContext context)
                throws JobExecutionException
        {
            try
            {
                context.setResult(this.methodInvoker.invoke());
            }
            catch (InvocationTargetException ex)
            {
                if ((ex.getTargetException() instanceof JobExecutionException)) {
                    throw ((JobExecutionException)ex.getTargetException());
                }
                throw new JobMethodInvocationFailedException(this.methodInvoker, ex.getTargetException());
            }
            catch (Exception ex)
            {
                throw new JobMethodInvocationFailedException(this.methodInvoker, ex);
            }
        }
    }
}
