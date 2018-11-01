package org.springframework.mail;

public class MailPreparationException
        extends MailException
{
    public MailPreparationException(String msg)
    {
        super(msg);
    }

    public MailPreparationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public MailPreparationException(Throwable cause)
    {
        super("Could not prepare mail", cause);
    }
}
