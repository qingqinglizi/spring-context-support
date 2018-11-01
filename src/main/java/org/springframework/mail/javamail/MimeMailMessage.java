package org.springframework.mail.javamail;

import java.util.Date;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailParseException;

public class MimeMailMessage
        implements MailMessage
{
    private final MimeMessageHelper helper;

    public MimeMailMessage(MimeMessageHelper mimeMessageHelper)
    {
        this.helper = mimeMessageHelper;
    }

    public MimeMailMessage(MimeMessage mimeMessage)
    {
        this.helper = new MimeMessageHelper(mimeMessage);
    }

    public final MimeMessageHelper getMimeMessageHelper()
    {
        return this.helper;
    }

    public final MimeMessage getMimeMessage()
    {
        return this.helper.getMimeMessage();
    }

    public void setFrom(String from)
            throws MailParseException
    {
        try
        {
            this.helper.setFrom(from);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setReplyTo(String replyTo)
            throws MailParseException
    {
        try
        {
            this.helper.setReplyTo(replyTo);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setTo(String to)
            throws MailParseException
    {
        try
        {
            this.helper.setTo(to);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setTo(String[] to)
            throws MailParseException
    {
        try
        {
            this.helper.setTo(to);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setCc(String cc)
            throws MailParseException
    {
        try
        {
            this.helper.setCc(cc);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setCc(String[] cc)
            throws MailParseException
    {
        try
        {
            this.helper.setCc(cc);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setBcc(String bcc)
            throws MailParseException
    {
        try
        {
            this.helper.setBcc(bcc);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setBcc(String[] bcc)
            throws MailParseException
    {
        try
        {
            this.helper.setBcc(bcc);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setSentDate(Date sentDate)
            throws MailParseException
    {
        try
        {
            this.helper.setSentDate(sentDate);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setSubject(String subject)
            throws MailParseException
    {
        try
        {
            this.helper.setSubject(subject);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }

    public void setText(String text)
            throws MailParseException
    {
        try
        {
            this.helper.setText(text);
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
    }
}
