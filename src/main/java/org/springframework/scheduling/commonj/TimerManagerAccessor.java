package org.springframework.scheduling.commonj;

import commonj.timers.TimerManager;
import javax.naming.NamingException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;

public abstract class TimerManagerAccessor
        extends JndiLocatorSupport
        implements InitializingBean, DisposableBean, Lifecycle
{
    private TimerManager timerManager;
    private String timerManagerName;
    private boolean shared = false;

    public void setTimerManager(TimerManager timerManager)
    {
        this.timerManager = timerManager;
    }

    public void setTimerManagerName(String timerManagerName)
    {
        this.timerManagerName = timerManagerName;
    }

    public void setShared(boolean shared)
    {
        this.shared = shared;
    }

    public void afterPropertiesSet()
            throws NamingException
    {
        if (this.timerManager == null)
        {
            if (this.timerManagerName == null) {
                throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
            }
            this.timerManager = ((TimerManager)lookup(this.timerManagerName, TimerManager.class));
        }
    }

    protected final TimerManager getTimerManager()
    {
        return this.timerManager;
    }

    public void start()
    {
        if (!this.shared) {
            this.timerManager.resume();
        }
    }

    public void stop()
    {
        if (!this.shared) {
            this.timerManager.suspend();
        }
    }

    public boolean isRunning()
    {
        return (!this.timerManager.isSuspending()) && (!this.timerManager.isStopping());
    }

    public void destroy()
    {
        if (!this.shared) {
            this.timerManager.stop();
        }
    }
}
