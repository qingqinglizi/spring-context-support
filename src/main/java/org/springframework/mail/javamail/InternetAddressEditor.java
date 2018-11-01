package org.springframework.mail.javamail;

import java.beans.PropertyEditorSupport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.springframework.util.StringUtils;

public class InternetAddressEditor
        extends PropertyEditorSupport
{
    public void setAsText(String text)
            throws IllegalArgumentException
    {
        if (StringUtils.hasText(text)) {
            try
            {
                setValue(new InternetAddress(text));
            }
            catch (AddressException ex)
            {
                throw new IllegalArgumentException("Could not parse mail address: " + ex.getMessage());
            }
        } else {
            setValue(null);
        }
    }

    public String getAsText()
    {
        InternetAddress value = (InternetAddress)getValue();
        return value != null ? value.toUnicodeString() : "";
    }
}
