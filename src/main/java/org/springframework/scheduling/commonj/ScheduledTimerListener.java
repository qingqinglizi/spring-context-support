package org.springframework.scheduling.commonj;

import commonj.timers.TimerListener;

public class ScheduledTimerListener
{
    private TimerListener timerListener;
    private long delay = 0L;
    private long period = -1L;
    private boolean fixedRate = false;

    public ScheduledTimerListener() {}

    public ScheduledTimerListener(TimerListener timerListener)
    {
        this.timerListener = timerListener;
    }

    public ScheduledTimerListener(TimerListener timerListener, long delay)
    {
        this.timerListener = timerListener;
        this.delay = delay;
    }

    public ScheduledTimerListener(TimerListener timerListener, long delay, long period, boolean fixedRate)
    {
        this.timerListener = timerListener;
        this.delay = delay;
        this.period = period;
        this.fixedRate = fixedRate;
    }

    public ScheduledTimerListener(Runnable timerTask)
    {
        setRunnable(timerTask);
    }

    public ScheduledTimerListener(Runnable timerTask, long delay)
    {
        setRunnable(timerTask);
        this.delay = delay;
    }

    public ScheduledTimerListener(Runnable timerTask, long delay, long period, boolean fixedRate)
    {
        setRunnable(timerTask);
        this.delay = delay;
        this.period = period;
        this.fixedRate = fixedRate;
    }

    public void setRunnable(Runnable timerTask)
    {
        this.timerListener = new DelegatingTimerListener(timerTask);
    }

    public void setTimerListener(TimerListener timerListener)
    {
        this.timerListener = timerListener;
    }

    public TimerListener getTimerListener()
    {
        return this.timerListener;
    }

    public void setDelay(long delay)
    {
        this.delay = delay;
    }

    public long getDelay()
    {
        return this.delay;
    }

    public void setPeriod(long period)
    {
        this.period = period;
    }

    public long getPeriod()
    {
        return this.period;
    }

    public boolean isOneTimeTask()
    {
        return this.period < 0L;
    }

    public void setFixedRate(boolean fixedRate)
    {
        this.fixedRate = fixedRate;
    }

    public boolean isFixedRate()
    {
        return this.fixedRate;
    }
}
