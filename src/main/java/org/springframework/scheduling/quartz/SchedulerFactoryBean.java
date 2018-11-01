package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.CollectionUtils;

public class SchedulerFactoryBean
        extends SchedulerAccessor
        implements FactoryBean<Scheduler>, BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle
{
    public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";
    public static final int DEFAULT_THREAD_COUNT = 10;
    private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder = new ThreadLocal();
    private static final ThreadLocal<Executor> configTimeTaskExecutorHolder = new ThreadLocal();
    private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal();
    private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder = new ThreadLocal();
    private SchedulerFactory schedulerFactory;

    public static ResourceLoader getConfigTimeResourceLoader()
    {
        return (ResourceLoader)configTimeResourceLoaderHolder.get();
    }

    public static Executor getConfigTimeTaskExecutor()
    {
        return (Executor)configTimeTaskExecutorHolder.get();
    }

    public static DataSource getConfigTimeDataSource()
    {
        return (DataSource)configTimeDataSourceHolder.get();
    }

    public static DataSource getConfigTimeNonTransactionalDataSource()
    {
        return (DataSource)configTimeNonTransactionalDataSourceHolder.get();
    }

    private Class<? extends SchedulerFactory> schedulerFactoryClass = StdSchedulerFactory.class;
    private String schedulerName;
    private Resource configLocation;
    private Properties quartzProperties;
    private Executor taskExecutor;
    private DataSource dataSource;
    private DataSource nonTransactionalDataSource;
    private Map<String, ?> schedulerContextMap;
    private ApplicationContext applicationContext;
    private String applicationContextSchedulerContextKey;
    private JobFactory jobFactory;
    private boolean jobFactorySet = false;
    private boolean autoStartup = true;
    private int startupDelay = 0;
    private int phase = 2147483647;
    private boolean exposeSchedulerInRepository = false;
    private boolean waitForJobsToCompleteOnShutdown = false;
    private Scheduler scheduler;

    public void setSchedulerFactory(SchedulerFactory schedulerFactory)
    {
        this.schedulerFactory = schedulerFactory;
    }

    public void setSchedulerFactoryClass(Class<? extends SchedulerFactory> schedulerFactoryClass)
    {
        this.schedulerFactoryClass = schedulerFactoryClass;
    }

    public void setSchedulerName(String schedulerName)
    {
        this.schedulerName = schedulerName;
    }

    public void setConfigLocation(Resource configLocation)
    {
        this.configLocation = configLocation;
    }

    public void setQuartzProperties(Properties quartzProperties)
    {
        this.quartzProperties = quartzProperties;
    }

    public void setTaskExecutor(Executor taskExecutor)
    {
        this.taskExecutor = taskExecutor;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource)
    {
        this.nonTransactionalDataSource = nonTransactionalDataSource;
    }

    public void setSchedulerContextAsMap(Map<String, ?> schedulerContextAsMap)
    {
        this.schedulerContextMap = schedulerContextAsMap;
    }

    public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey)
    {
        this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
    }

    public void setJobFactory(JobFactory jobFactory)
    {
        this.jobFactory = jobFactory;
        this.jobFactorySet = true;
    }

    public void setAutoStartup(boolean autoStartup)
    {
        this.autoStartup = autoStartup;
    }

    public boolean isAutoStartup()
    {
        return this.autoStartup;
    }

    public void setPhase(int phase)
    {
        this.phase = phase;
    }

    public int getPhase()
    {
        return this.phase;
    }

    public void setStartupDelay(int startupDelay)
    {
        this.startupDelay = startupDelay;
    }

    public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository)
    {
        this.exposeSchedulerInRepository = exposeSchedulerInRepository;
    }

    public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown)
    {
        this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
    }

    public void setBeanName(String name)
    {
        if (this.schedulerName == null) {
            this.schedulerName = name;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public void afterPropertiesSet()
            throws Exception
    {
        if ((this.dataSource == null) && (this.nonTransactionalDataSource != null)) {
            this.dataSource = this.nonTransactionalDataSource;
        }
        if ((this.applicationContext != null) && (this.resourceLoader == null)) {
            this.resourceLoader = this.applicationContext;
        }
        this.scheduler = prepareScheduler(prepareSchedulerFactory());
        try
        {
            registerListeners();
            registerJobsAndTriggers();
        }
        catch (Exception ex)
        {
            try
            {
                this.scheduler.shutdown(true);
            }
            catch (Exception ex2)
            {
                this.logger.debug("Scheduler shutdown exception after registration failure", ex2);
            }
            throw ex;
        }
    }

    private SchedulerFactory prepareSchedulerFactory()
            throws SchedulerException, IOException
    {
        SchedulerFactory schedulerFactory = this.schedulerFactory;
        if (schedulerFactory == null)
        {
            schedulerFactory = (SchedulerFactory)BeanUtils.instantiateClass(this.schedulerFactoryClass);
            if ((schedulerFactory instanceof StdSchedulerFactory)) {
                initSchedulerFactory((StdSchedulerFactory)schedulerFactory);
            } else if ((this.configLocation != null) || (this.quartzProperties != null) || (this.taskExecutor != null) || (this.dataSource != null)) {
                throw new IllegalArgumentException("StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
            }
        }
        return schedulerFactory;
    }

    private void initSchedulerFactory(StdSchedulerFactory schedulerFactory)
            throws SchedulerException, IOException
    {
        Properties mergedProps = new Properties();
        if (this.resourceLoader != null) {
            mergedProps.setProperty("org.quartz.scheduler.classLoadHelper.class", ResourceLoaderClassLoadHelper.class
                    .getName());
        }
        if (this.taskExecutor != null)
        {
            mergedProps.setProperty("org.quartz.threadPool.class", LocalTaskExecutorThreadPool.class
                    .getName());
        }
        else
        {
            mergedProps.setProperty("org.quartz.threadPool.class", SimpleThreadPool.class.getName());
            mergedProps.setProperty("org.quartz.threadPool.threadCount", Integer.toString(10));
        }
        if (this.configLocation != null)
        {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Loading Quartz config from [" + this.configLocation + "]");
            }
            PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
        }
        CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
        if (this.dataSource != null) {
            mergedProps.put("org.quartz.jobStore.class", LocalDataSourceJobStore.class.getName());
        }
        if (this.schedulerName != null) {
            mergedProps.put("org.quartz.scheduler.instanceName", this.schedulerName);
        }
        schedulerFactory.initialize(mergedProps);
    }

    private Scheduler prepareScheduler(SchedulerFactory schedulerFactory)
            throws SchedulerException
    {
        if (this.resourceLoader != null) {
            configTimeResourceLoaderHolder.set(this.resourceLoader);
        }
        if (this.taskExecutor != null) {
            configTimeTaskExecutorHolder.set(this.taskExecutor);
        }
        if (this.dataSource != null) {
            configTimeDataSourceHolder.set(this.dataSource);
        }
        if (this.nonTransactionalDataSource != null) {
            configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
        }
        try
        {
            Scheduler scheduler = createScheduler(schedulerFactory, this.schedulerName);
            populateSchedulerContext(scheduler);
            if ((!this.jobFactorySet) && (!(scheduler instanceof RemoteScheduler))) {
                this.jobFactory = new AdaptableJobFactory();
            }
            if (this.jobFactory != null)
            {
                if ((this.jobFactory instanceof SchedulerContextAware)) {
                    ((SchedulerContextAware)this.jobFactory).setSchedulerContext(scheduler.getContext());
                }
                scheduler.setJobFactory(this.jobFactory);
            }
            return scheduler;
        }
        finally
        {
            if (this.resourceLoader != null) {
                configTimeResourceLoaderHolder.remove();
            }
            if (this.taskExecutor != null) {
                configTimeTaskExecutorHolder.remove();
            }
            if (this.dataSource != null) {
                configTimeDataSourceHolder.remove();
            }
            if (this.nonTransactionalDataSource != null) {
                configTimeNonTransactionalDataSourceHolder.remove();
            }
        }
    }

    /* Error */
    protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName)
            throws SchedulerException
    {
        // Byte code:
        //   0: invokestatic 87	java/lang/Thread:currentThread	()Ljava/lang/Thread;
        //   3: astore_3
        //   4: aload_3
        //   5: invokevirtual 88	java/lang/Thread:getContextClassLoader	()Ljava/lang/ClassLoader;
        //   8: astore 4
        //   10: aload_0
        //   11: getfield 31	org/springframework/scheduling/quartz/SchedulerFactoryBean:resourceLoader	Lorg/springframework/core/io/ResourceLoader;
        //   14: ifnull +21 -> 35
        //   17: aload_0
        //   18: getfield 31	org/springframework/scheduling/quartz/SchedulerFactoryBean:resourceLoader	Lorg/springframework/core/io/ResourceLoader;
        //   21: invokeinterface 89 1 0
        //   26: aload 4
        //   28: if_acmpeq +7 -> 35
        //   31: iconst_1
        //   32: goto +4 -> 36
        //   35: iconst_0
        //   36: istore 5
        //   38: iload 5
        //   40: ifeq +16 -> 56
        //   43: aload_3
        //   44: aload_0
        //   45: getfield 31	org/springframework/scheduling/quartz/SchedulerFactoryBean:resourceLoader	Lorg/springframework/core/io/ResourceLoader;
        //   48: invokeinterface 89 1 0
        //   53: invokevirtual 90	java/lang/Thread:setContextClassLoader	(Ljava/lang/ClassLoader;)V
        //   56: invokestatic 91	org/quartz/impl/SchedulerRepository:getInstance	()Lorg/quartz/impl/SchedulerRepository;
        //   59: astore 6
        //   61: aload 6
        //   63: dup
        //   64: astore 7
        //   66: monitorenter
        //   67: aload_2
        //   68: ifnull +12 -> 80
        //   71: aload 6
        //   73: aload_2
        //   74: invokevirtual 92	org/quartz/impl/SchedulerRepository:lookup	(Ljava/lang/String;)Lorg/quartz/Scheduler;
        //   77: goto +4 -> 81
        //   80: aconst_null
        //   81: astore 8
        //   83: aload_1
        //   84: invokeinterface 93 1 0
        //   89: astore 9
        //   91: aload 9
        //   93: aload 8
        //   95: if_acmpne +35 -> 130
        //   98: new 94	java/lang/IllegalStateException
        //   101: dup
        //   102: new 46	java/lang/StringBuilder
        //   105: dup
        //   106: invokespecial 47	java/lang/StringBuilder:<init>	()V
        //   109: ldc 95
        //   111: invokevirtual 49	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   114: aload_2
        //   115: invokevirtual 49	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   118: ldc 96
        //   120: invokevirtual 49	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //   123: invokevirtual 51	java/lang/StringBuilder:toString	()Ljava/lang/String;
        //   126: invokespecial 97	java/lang/IllegalStateException:<init>	(Ljava/lang/String;)V
        //   129: athrow
        //   130: aload_0
        //   131: getfield 10	org/springframework/scheduling/quartz/SchedulerFactoryBean:exposeSchedulerInRepository	Z
        //   134: ifne +17 -> 151
        //   137: invokestatic 91	org/quartz/impl/SchedulerRepository:getInstance	()Lorg/quartz/impl/SchedulerRepository;
        //   140: aload 9
        //   142: invokeinterface 98 1 0
        //   147: invokevirtual 99	org/quartz/impl/SchedulerRepository:remove	(Ljava/lang/String;)Z
        //   150: pop
        //   151: aload 9
        //   153: astore 10
        //   155: aload 7
        //   157: monitorexit
        //   158: iload 5
        //   160: ifeq +9 -> 169
        //   163: aload_3
        //   164: aload 4
        //   166: invokevirtual 90	java/lang/Thread:setContextClassLoader	(Ljava/lang/ClassLoader;)V
        //   169: aload 10
        //   171: areturn
        //   172: astore 11
        //   174: aload 7
        //   176: monitorexit
        //   177: aload 11
        //   179: athrow
        //   180: astore 12
        //   182: iload 5
        //   184: ifeq +9 -> 193
        //   187: aload_3
        //   188: aload 4
        //   190: invokevirtual 90	java/lang/Thread:setContextClassLoader	(Ljava/lang/ClassLoader;)V
        //   193: aload 12
        //   195: athrow
        // Line number table:
        //   Java source line #623	-> byte code offset #0
        //   Java source line #624	-> byte code offset #4
        //   Java source line #625	-> byte code offset #10
        //   Java source line #626	-> byte code offset #21
        //   Java source line #627	-> byte code offset #38
        //   Java source line #628	-> byte code offset #43
        //   Java source line #631	-> byte code offset #56
        //   Java source line #632	-> byte code offset #61
        //   Java source line #633	-> byte code offset #67
        //   Java source line #634	-> byte code offset #83
        //   Java source line #635	-> byte code offset #91
        //   Java source line #636	-> byte code offset #98
        //   Java source line #639	-> byte code offset #130
        //   Java source line #641	-> byte code offset #137
        //   Java source line #643	-> byte code offset #151
        //   Java source line #647	-> byte code offset #158
        //   Java source line #649	-> byte code offset #163
        //   Java source line #643	-> byte code offset #169
        //   Java source line #644	-> byte code offset #172
        //   Java source line #647	-> byte code offset #180
        //   Java source line #649	-> byte code offset #187
        // Local variable table:
        //   start	length	slot	name	signature
        //   0	196	0	this	SchedulerFactoryBean
        //   0	196	1	schedulerFactory	SchedulerFactory
        //   0	196	2	schedulerName	String
        //   3	185	3	currentThread	Thread
        //   8	181	4	threadContextClassLoader	java.lang.ClassLoader
        //   36	147	5	overrideClassLoader	boolean
        //   59	13	6	repository	org.quartz.impl.SchedulerRepository
        //   81	13	8	existingScheduler	Scheduler
        //   89	63	9	newScheduler	Scheduler
        //   153	17	10	localScheduler1	Scheduler
        //   172	6	11	localObject1	Object
        //   180	14	12	localObject2	Object
        // Exception table:
        //   from	to	target	type
        //   67	158	172	finally
        //   172	177	172	finally
        //   56	158	180	finally
        //   172	182	180	finally
    }

    private void populateSchedulerContext(Scheduler scheduler)
            throws SchedulerException
    {
        if (this.schedulerContextMap != null) {
            scheduler.getContext().putAll(this.schedulerContextMap);
        }
        if (this.applicationContextSchedulerContextKey != null)
        {
            if (this.applicationContext == null) {
                throw new IllegalStateException("SchedulerFactoryBean needs to be set up in an ApplicationContext to be able to handle an 'applicationContextSchedulerContextKey'");
            }
            scheduler.getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
        }
    }

    protected void startScheduler(final Scheduler scheduler, final int startupDelay)
            throws SchedulerException
    {
        if (startupDelay <= 0)
        {
            this.logger.info("Starting Quartz Scheduler now");
            scheduler.start();
        }
        else
        {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() + "] in " + startupDelay + " seconds");
            }
            Thread schedulerThread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(startupDelay * 1000);
                    }
                    catch (InterruptedException ex)
                    {
                        Thread.currentThread().interrupt();
                    }
                    if (SchedulerFactoryBean.this.logger.isInfoEnabled()) {
                        SchedulerFactoryBean.this.logger.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
                    }
                    try
                    {
                        scheduler.start();
                    }
                    catch (SchedulerException ex)
                    {
                        throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
                    }
                }
            };
            schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
            schedulerThread.setDaemon(true);
            schedulerThread.start();
        }
    }

    public Scheduler getScheduler()
    {
        return this.scheduler;
    }

    public Scheduler getObject()
    {
        return this.scheduler;
    }

    public Class<? extends Scheduler> getObjectType()
    {
        return this.scheduler != null ? this.scheduler.getClass() : Scheduler.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void start()
            throws SchedulingException
    {
        if (this.scheduler != null) {
            try
            {
                startScheduler(this.scheduler, this.startupDelay);
            }
            catch (SchedulerException ex)
            {
                throw new SchedulingException("Could not start Quartz Scheduler", ex);
            }
        }
    }

    public void stop()
            throws SchedulingException
    {
        if (this.scheduler != null) {
            try
            {
                this.scheduler.standby();
            }
            catch (SchedulerException ex)
            {
                throw new SchedulingException("Could not stop Quartz Scheduler", ex);
            }
        }
    }

    public void stop(Runnable callback)
            throws SchedulingException
    {
        stop();
        callback.run();
    }

    public boolean isRunning()
            throws SchedulingException
    {
        if (this.scheduler != null) {
            try
            {
                return !this.scheduler.isInStandbyMode();
            }
            catch (SchedulerException ex)
            {
                return false;
            }
        }
        return false;
    }

    public void destroy()
            throws SchedulerException
    {
        if (this.scheduler != null)
        {
            this.logger.info("Shutting down Quartz Scheduler");
            this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
        }
    }
}
