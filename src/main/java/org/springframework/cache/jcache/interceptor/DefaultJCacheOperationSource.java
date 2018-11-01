package org.springframework.cache.jcache.interceptor;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.util.Assert;

public class DefaultJCacheOperationSource
        extends AnnotationJCacheOperationSource
        implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton
{
    private CacheManager cacheManager;
    private CacheResolver cacheResolver;
    private CacheResolver exceptionCacheResolver;
    private KeyGenerator keyGenerator = new SimpleKeyGenerator();
    private KeyGenerator adaptedKeyGenerator;
    private BeanFactory beanFactory;

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager()
    {
        return this.cacheManager;
    }

    public void setCacheResolver(CacheResolver cacheResolver)
    {
        this.cacheResolver = cacheResolver;
    }

    public CacheResolver getCacheResolver()
    {
        return this.cacheResolver;
    }

    public void setExceptionCacheResolver(CacheResolver exceptionCacheResolver)
    {
        this.exceptionCacheResolver = exceptionCacheResolver;
    }

    public CacheResolver getExceptionCacheResolver()
    {
        return this.exceptionCacheResolver;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator)
    {
        this.keyGenerator = keyGenerator;
    }

    public KeyGenerator getKeyGenerator()
    {
        return this.keyGenerator;
    }

    public void setBeanFactory(BeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    public void afterPropertiesSet()
    {
        this.adaptedKeyGenerator = new KeyGeneratorAdapter(this, this.keyGenerator);
    }

    public void afterSingletonsInstantiated()
    {
        Assert.notNull(getDefaultCacheResolver(), "Cache resolver should have been initialized");
    }

    protected <T> T getBean(Class<T> type)
    {
        try
        {
            return this.beanFactory.getBean(type);
        }
        catch (NoUniqueBeanDefinitionException ex)
        {
            throw new IllegalStateException("No unique [" + type.getName() + "] bean found in application context - mark one as primary, or declare a more specific implementation type for your cache", ex);
        }
        catch (NoSuchBeanDefinitionException ex)
        {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No bean of type [" + type.getName() + "] found in application context", ex);
            }
        }
        return BeanUtils.instantiateClass(type);
    }

    protected CacheManager getDefaultCacheManager()
    {
        if (this.cacheManager == null) {
            try
            {
                this.cacheManager = ((CacheManager)this.beanFactory.getBean(CacheManager.class));
            }
            catch (NoUniqueBeanDefinitionException ex)
            {
                throw new IllegalStateException("No unique bean of type CacheManager found. Mark one as primary or declare a specific CacheManager to use.");
            }
            catch (NoSuchBeanDefinitionException ex)
            {
                throw new IllegalStateException("No bean of type CacheManager found. Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
            }
        }
        return this.cacheManager;
    }

    protected CacheResolver getDefaultCacheResolver()
    {
        if (this.cacheResolver == null) {
            this.cacheResolver = new SimpleCacheResolver(getDefaultCacheManager());
        }
        return this.cacheResolver;
    }

    protected CacheResolver getDefaultExceptionCacheResolver()
    {
        if (this.exceptionCacheResolver == null) {
            this.exceptionCacheResolver = new LazyCacheResolver();
        }
        return this.exceptionCacheResolver;
    }

    protected KeyGenerator getDefaultKeyGenerator()
    {
        return this.adaptedKeyGenerator;
    }

    class LazyCacheResolver
            implements CacheResolver
    {
        private CacheResolver cacheResolver;

        LazyCacheResolver() {}

        public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context)
        {
            if (this.cacheResolver == null) {
                this.cacheResolver = new SimpleExceptionCacheResolver(DefaultJCacheOperationSource.this.getDefaultCacheManager());
            }
            return this.cacheResolver.resolveCaches(context);
        }
    }
}
