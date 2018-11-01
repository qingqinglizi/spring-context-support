package org.springframework.ui.freemarker;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;

public abstract class FreeMarkerTemplateUtils
{
    public static String processTemplateIntoString(Template template, Object model)
            throws IOException, TemplateException
    {
        StringWriter result = new StringWriter();
        template.process(model, result);
        return result.toString();
    }
}
