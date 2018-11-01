package org.springframework.mail;

import org.springframework.core.NestedRuntimeException;

public abstract class MailException
        extends NestedRuntimeException
{
    public MailException(String msg)
    {
        super(msg);
    }

    public MailException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
