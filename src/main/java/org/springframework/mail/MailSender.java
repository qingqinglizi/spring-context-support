package org.springframework.mail;

public abstract interface MailSender
{
    public abstract void send(SimpleMailMessage paramSimpleMailMessage)
            throws MailException;

    public abstract void send(SimpleMailMessage... paramVarArgs)
            throws MailException;
}
