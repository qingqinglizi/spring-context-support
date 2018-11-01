package org.springframework.ui.freemarker;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

public class FreeMarkerConfigurationFactoryBean
        extends FreeMarkerConfigurationFactory
        implements FactoryBean<Configuration>, InitializingBean, ResourceLoaderAware
{
    private Configuration configuration;

    public void afterPropertiesSet()
            throws IOException, TemplateException
    {
        this.configuration = createConfiguration();
    }

    public Configuration getObject()
    {
        return this.configuration;
    }

    public Class<? extends Configuration> getObjectType()
    {
        return Configuration.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
