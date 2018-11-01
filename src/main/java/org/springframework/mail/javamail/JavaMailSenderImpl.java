package org.springframework.mail.javamail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.FileTypeMap;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;

public class JavaMailSenderImpl
        implements JavaMailSender
{
    public static final String DEFAULT_PROTOCOL = "smtp";
    public static final int DEFAULT_PORT = -1;
    private static final String HEADER_MESSAGE_ID = "Message-ID";
    private Properties javaMailProperties = new Properties();
    private Session session;
    private String protocol;
    private String host;
    private int port = -1;
    private String username;
    private String password;
    private String defaultEncoding;
    private FileTypeMap defaultFileTypeMap;

    public JavaMailSenderImpl()
    {
        ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
        fileTypeMap.afterPropertiesSet();
        this.defaultFileTypeMap = fileTypeMap;
    }

    public void setJavaMailProperties(Properties javaMailProperties)
    {
        this.javaMailProperties = javaMailProperties;
        synchronized (this)
        {
            this.session = null;
        }
    }

    public Properties getJavaMailProperties()
    {
        return this.javaMailProperties;
    }

    public synchronized void setSession(Session session)
    {
        Assert.notNull(session, "Session must not be null");
        this.session = session;
    }

    public synchronized Session getSession()
    {
        if (this.session == null) {
            this.session = Session.getInstance(this.javaMailProperties);
        }
        return this.session;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public String getProtocol()
    {
        return this.protocol;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getHost()
    {
        return this.host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return this.port;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getUsername()
    {
        return this.username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return this.password;
    }

    public void setDefaultEncoding(String defaultEncoding)
    {
        this.defaultEncoding = defaultEncoding;
    }

    public String getDefaultEncoding()
    {
        return this.defaultEncoding;
    }

    public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap)
    {
        this.defaultFileTypeMap = defaultFileTypeMap;
    }

    public FileTypeMap getDefaultFileTypeMap()
    {
        return this.defaultFileTypeMap;
    }

    public void send(SimpleMailMessage simpleMessage)
            throws MailException
    {
        send(new SimpleMailMessage[] { simpleMessage });
    }

    public void send(SimpleMailMessage... simpleMessages)
            throws MailException
    {
        List<MimeMessage> mimeMessages = new ArrayList(simpleMessages.length);
        for (SimpleMailMessage simpleMessage : simpleMessages)
        {
            MimeMailMessage message = new MimeMailMessage(createMimeMessage());
            simpleMessage.copyTo(message);
            mimeMessages.add(message.getMimeMessage());
        }
        doSend((MimeMessage[])mimeMessages.toArray(new MimeMessage[mimeMessages.size()]), simpleMessages);
    }

    public MimeMessage createMimeMessage()
    {
        return new SmartMimeMessage(getSession(), getDefaultEncoding(), getDefaultFileTypeMap());
    }

    public MimeMessage createMimeMessage(InputStream contentStream)
            throws MailException
    {
        try
        {
            return new MimeMessage(getSession(), contentStream);
        }
        catch (Exception ex)
        {
            throw new MailParseException("Could not parse raw MIME content", ex);
        }
    }

    public void send(MimeMessage mimeMessage)
            throws MailException
    {
        send(new MimeMessage[] { mimeMessage });
    }

    public void send(MimeMessage... mimeMessages)
            throws MailException
    {
        doSend(mimeMessages, null);
    }

    public void send(MimeMessagePreparator mimeMessagePreparator)
            throws MailException
    {
        send(new MimeMessagePreparator[] { mimeMessagePreparator });
    }

    public void send(MimeMessagePreparator... mimeMessagePreparators)
            throws MailException
    {
        try
        {
            List<MimeMessage> mimeMessages = new ArrayList(mimeMessagePreparators.length);
            for (MimeMessagePreparator preparator : mimeMessagePreparators)
            {
                MimeMessage mimeMessage = createMimeMessage();
                preparator.prepare(mimeMessage);
                mimeMessages.add(mimeMessage);
            }
            send((MimeMessage[])mimeMessages.toArray(new MimeMessage[mimeMessages.size()]));
        }
        catch (MailException ex)
        {
            throw ex;
        }
        catch (MessagingException ex)
        {
            throw new MailParseException(ex);
        }
        catch (Exception ex)
        {
            throw new MailPreparationException(ex);
        }
    }

    public void testConnection()
            throws MessagingException
    {
        Transport transport = null;
        try
        {
            transport = connectTransport();
            if (transport != null) {
                transport.close();
            }
        }
        finally
        {
            if (transport != null) {
                transport.close();
            }
        }
    }

    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages)
            throws MailException
    {
        Map<Object, Exception> failedMessages = new LinkedHashMap();
        Transport transport = null;
        try
        {
            for (int i = 0; i < mimeMessages.length; i++)
            {
                if ((transport == null) || (!transport.isConnected()))
                {
                    if (transport != null)
                    {
                        try
                        {
                            transport.close();
                        }
                        catch (Exception localException1) {}
                        transport = null;
                    }
                    try
                    {
                        transport = connectTransport();
                    }
                    catch (AuthenticationFailedException ex)
                    {
                        throw new MailAuthenticationException(ex);
                    }
                    catch (Exception ex)
                    {
                        for (int j = i; j < mimeMessages.length; j++)
                        {
                            Object original = originalMessages != null ? originalMessages[j] : mimeMessages[j];
                            failedMessages.put(original, ex);
                        }
                        throw new MailSendException("Mail server connection failed", ex, failedMessages);
                    }
                }
                MimeMessage mimeMessage = mimeMessages[i];
                try
                {
                    if (mimeMessage.getSentDate() == null) {
                        mimeMessage.setSentDate(new Date());
                    }
                    String messageId = mimeMessage.getMessageID();
                    mimeMessage.saveChanges();
                    if (messageId != null) {
                        mimeMessage.setHeader("Message-ID", messageId);
                    }
                    transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                }
                catch (Exception ex)
                {
                    Object original = originalMessages != null ? originalMessages[i] : mimeMessage;
                    failedMessages.put(original, ex);
                }
            }
            try
            {
                if (transport != null) {
                    transport.close();
                }
            }
            catch (Exception ex)
            {
                if (!failedMessages.isEmpty()) {
                    throw new MailSendException("Failed to close server connection after message failures", ex, failedMessages);
                }
                throw new MailSendException("Failed to close server connection after message sending", ex);
            }
            if (failedMessages.isEmpty()) {
                return;
            }
        }
        finally
        {
            try
            {
                if (transport != null) {
                    transport.close();
                }
            }
            catch (Exception ex)
            {
                if (!failedMessages.isEmpty()) {
                    throw new MailSendException("Failed to close server connection after message failures", ex, failedMessages);
                }
                throw new MailSendException("Failed to close server connection after message sending", ex);
            }
        }
        throw new MailSendException(failedMessages);
    }

    protected Transport connectTransport()
            throws MessagingException
    {
        String username = getUsername();
        String password = getPassword();
        if ("".equals(username))
        {
            username = null;
            if ("".equals(password)) {
                password = null;
            }
        }
        Transport transport = getTransport(getSession());
        transport.connect(getHost(), getPort(), username, password);
        return transport;
    }

    protected Transport getTransport(Session session)
            throws NoSuchProviderException
    {
        String protocol = getProtocol();
        if (protocol == null)
        {
            protocol = session.getProperty("mail.transport.protocol");
            if (protocol == null) {
                protocol = "smtp";
            }
        }
        return session.getTransport(protocol);
    }
}
