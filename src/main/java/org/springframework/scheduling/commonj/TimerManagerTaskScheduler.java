package org.springframework.scheduling.commonj;

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;

public class TimerManagerTaskScheduler
        extends TimerManagerAccessor
        implements TaskScheduler
{
    private volatile ErrorHandler errorHandler;

    public void setErrorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger)
    {
        return new ReschedulingTimerListener(errorHandlingTask(task, true), trigger).schedule();
    }

    public ScheduledFuture<?> schedule(Runnable task, Date startTime)
    {
        TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, false));
        Timer timer = getTimerManager().schedule(futureTask, startTime);
        futureTask.setTimer(timer);
        return futureTask;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period)
    {
        TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
        Timer timer = getTimerManager().scheduleAtFixedRate(futureTask, startTime, period);
        futureTask.setTimer(timer);
        return futureTask;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period)
    {
        TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
        Timer timer = getTimerManager().scheduleAtFixedRate(futureTask, 0L, period);
        futureTask.setTimer(timer);
        return futureTask;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay)
    {
        TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
        Timer timer = getTimerManager().schedule(futureTask, startTime, delay);
        futureTask.setTimer(timer);
        return futureTask;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay)
    {
        TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
        Timer timer = getTimerManager().schedule(futureTask, 0L, delay);
        futureTask.setTimer(timer);
        return futureTask;
    }

    private Runnable errorHandlingTask(Runnable delegate, boolean isRepeatingTask)
    {
        return TaskUtils.decorateTaskWithErrorHandler(delegate, this.errorHandler, isRepeatingTask);
    }

    private static class TimerScheduledFuture
            extends FutureTask<Object>
            implements TimerListener, ScheduledFuture<Object>
    {
        protected transient Timer timer;
        protected transient boolean cancelled = false;

        public TimerScheduledFuture(Runnable runnable)
        {
            super(null);
        }

        public void setTimer(Timer timer)
        {
            this.timer = timer;
        }

        public void timerExpired(Timer timer)
        {
            runAndReset();
        }

        public boolean cancel(boolean mayInterruptIfRunning)
        {
            boolean result = super.cancel(mayInterruptIfRunning);
            this.timer.cancel();
            this.cancelled = true;
            return result;
        }

        public long getDelay(TimeUnit unit)
        {
            return unit.convert(this.timer.getScheduledExecutionTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        public int compareTo(Delayed other)
        {
            if (this == other) {
                return 0;
            }
            long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return diff < 0L ? -1 : diff == 0L ? 0 : 1;
        }
    }

    private class ReschedulingTimerListener
            extends TimerManagerTaskScheduler.TimerScheduledFuture
    {
        private final Trigger trigger;
        private final SimpleTriggerContext triggerContext = new SimpleTriggerContext();
        private volatile Date scheduledExecutionTime;

        public ReschedulingTimerListener(Runnable runnable, Trigger trigger)
        {
            super();
            this.trigger = trigger;
        }

        public ScheduledFuture<?> schedule()
        {
            this.scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext);
            if (this.scheduledExecutionTime == null) {
                return null;
            }
            setTimer(TimerManagerTaskScheduler.this.getTimerManager().schedule(this, this.scheduledExecutionTime));
            return this;
        }

        public void timerExpired(Timer timer)
        {
            Date actualExecutionTime = new Date();
            super.timerExpired(timer);
            Date completionTime = new Date();
            this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
            if (!this.cancelled) {
                schedule();
            }
        }
    }
}
