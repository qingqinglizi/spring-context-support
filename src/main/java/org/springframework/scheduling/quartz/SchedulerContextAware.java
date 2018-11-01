package org.springframework.scheduling.quartz;

import org.quartz.SchedulerContext;
import org.springframework.beans.factory.Aware;

public abstract interface SchedulerContextAware
        extends Aware
{
  public abstract void setSchedulerContext(SchedulerContext paramSchedulerContext);
}
