package org.springframework.scheduling.quartz;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.MethodInvoker;

public class JobMethodInvocationFailedException
        extends NestedRuntimeException
{
    public JobMethodInvocationFailedException(MethodInvoker methodInvoker, Throwable cause)
    {
        super("Invocation of method '" + methodInvoker.getTargetMethod() + "' on target class [" + methodInvoker
                .getTargetClass() + "] failed", cause);
    }
}
