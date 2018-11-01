package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerManager;
import java.util.LinkedList;
import java.util.List;
import javax.naming.NamingException;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

public class TimerManagerFactoryBean
        extends TimerManagerAccessor
        implements FactoryBean<TimerManager>, InitializingBean, DisposableBean, Lifecycle
{
    private ScheduledTimerListener[] scheduledTimerListeners;
    private final List<Timer> timers = new LinkedList();

    public void setScheduledTimerListeners(ScheduledTimerListener[] scheduledTimerListeners)
    {
        this.scheduledTimerListeners = scheduledTimerListeners;
    }

    public void afterPropertiesSet()
            throws NamingException
    {
        super.afterPropertiesSet();
        if (this.scheduledTimerListeners != null)
        {
            TimerManager timerManager = getTimerManager();
            for (ScheduledTimerListener scheduledTask : this.scheduledTimerListeners)
            {
                Timer timer;
                Timer timer;
                if (scheduledTask.isOneTimeTask())
                {
                    timer = timerManager.schedule(scheduledTask.getTimerListener(), scheduledTask.getDelay());
                }
                else
                {
                    Timer timer;
                    if (scheduledTask.isFixedRate()) {
                        timer = timerManager.scheduleAtFixedRate(scheduledTask
                                .getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
                    } else {
                        timer = timerManager.schedule(scheduledTask
                                .getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
                    }
                }
                this.timers.add(timer);
            }
        }
    }

    public TimerManager getObject()
    {
        return getTimerManager();
    }

    public Class<? extends TimerManager> getObjectType()
    {
        TimerManager timerManager = getTimerManager();
        return timerManager != null ? timerManager.getClass() : TimerManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void destroy()
    {
        for (Timer timer : this.timers) {
            try
            {
                timer.cancel();
            }
            catch (Throwable ex)
            {
                this.logger.warn("Could not cancel CommonJ Timer", ex);
            }
        }
        this.timers.clear();


        super.destroy();
    }
}
