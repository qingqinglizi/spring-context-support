package org.springframework.mail.javamail;

import javax.mail.internet.MimeMessage;

public abstract interface MimeMessagePreparator
{
  public abstract void prepare(MimeMessage paramMimeMessage)
          throws Exception;
}
