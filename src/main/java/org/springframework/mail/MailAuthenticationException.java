package org.springframework.mail;

public class MailAuthenticationException
        extends MailException
{
    public MailAuthenticationException(String msg)
    {
        super(msg);
    }

    public MailAuthenticationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public MailAuthenticationException(Throwable cause)
    {
        super("Authentication failed", cause);
    }
}
