package org.springframework.mail.javamail;

import java.io.InputStream;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;

public abstract interface JavaMailSender
        extends MailSender
{
  public abstract MimeMessage createMimeMessage();

  public abstract MimeMessage createMimeMessage(InputStream paramInputStream)
          throws MailException;

  public abstract void send(MimeMessage paramMimeMessage)
          throws MailException;

  public abstract void send(MimeMessage... paramVarArgs)
          throws MailException;

  public abstract void send(MimeMessagePreparator paramMimeMessagePreparator)
          throws MailException;

  public abstract void send(MimeMessagePreparator... paramVarArgs)
          throws MailException;
}
