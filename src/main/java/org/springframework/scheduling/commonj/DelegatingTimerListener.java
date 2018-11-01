package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import org.springframework.util.Assert;

public class DelegatingTimerListener
        implements TimerListener
{
    private final Runnable runnable;

    public DelegatingTimerListener(Runnable runnable)
    {
        Assert.notNull(runnable, "Runnable is required");
        this.runnable = runnable;
    }

    public void timerExpired(Timer timer)
    {
        this.runnable.run();
    }
}
