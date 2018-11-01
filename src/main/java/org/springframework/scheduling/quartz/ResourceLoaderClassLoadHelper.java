package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.spi.ClassLoadHelper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ResourceLoaderClassLoadHelper
        implements ClassLoadHelper
{
    protected static final Log logger = LogFactory.getLog(ResourceLoaderClassLoadHelper.class);
    private ResourceLoader resourceLoader;

    public ResourceLoaderClassLoadHelper() {}

    public ResourceLoaderClassLoadHelper(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    public void initialize()
    {
        if (this.resourceLoader == null)
        {
            this.resourceLoader = SchedulerFactoryBean.getConfigTimeResourceLoader();
            if (this.resourceLoader == null) {
                this.resourceLoader = new DefaultResourceLoader();
            }
        }
    }

    public Class<?> loadClass(String name)
            throws ClassNotFoundException
    {
        return this.resourceLoader.getClassLoader().loadClass(name);
    }

    public <T> Class<? extends T> loadClass(String name, Class<T> clazz)
            throws ClassNotFoundException
    {
        return loadClass(name);
    }

    public URL getResource(String name)
    {
        Resource resource = this.resourceLoader.getResource(name);
        if (resource.exists()) {
            try
            {
                return resource.getURL();
            }
            catch (IOException ex)
            {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not load " + resource);
                }
                return null;
            }
        }
        return getClassLoader().getResource(name);
    }

    public InputStream getResourceAsStream(String name)
    {
        Resource resource = this.resourceLoader.getResource(name);
        if (resource.exists()) {
            try
            {
                return resource.getInputStream();
            }
            catch (IOException ex)
            {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not load " + resource);
                }
                return null;
            }
        }
        return getClassLoader().getResourceAsStream(name);
    }

    public ClassLoader getClassLoader()
    {
        return this.resourceLoader.getClassLoader();
    }
}
