package org.springframework.cache.jcache;

import java.net.URI;
import java.util.Properties;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class JCacheManagerFactoryBean
        implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean
{
    private URI cacheManagerUri;
    private Properties cacheManagerProperties;
    private ClassLoader beanClassLoader;
    private CacheManager cacheManager;

    public void setCacheManagerUri(URI cacheManagerUri)
    {
        this.cacheManagerUri = cacheManagerUri;
    }

    public void setCacheManagerProperties(Properties cacheManagerProperties)
    {
        this.cacheManagerProperties = cacheManagerProperties;
    }

    public void setBeanClassLoader(ClassLoader classLoader)
    {
        this.beanClassLoader = classLoader;
    }

    public void afterPropertiesSet()
    {
        this.cacheManager = Caching.getCachingProvider().getCacheManager(this.cacheManagerUri, this.beanClassLoader, this.cacheManagerProperties);
    }

    public CacheManager getObject()
    {
        return this.cacheManager;
    }

    public Class<?> getObjectType()
    {
        return this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void destroy()
    {
        this.cacheManager.close();
    }
}
