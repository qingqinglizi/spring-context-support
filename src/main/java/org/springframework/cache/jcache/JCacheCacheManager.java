package org.springframework.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;

public class JCacheCacheManager
        extends AbstractTransactionSupportingCacheManager
{
    private CacheManager cacheManager;
    private boolean allowNullValues = true;

    public JCacheCacheManager() {}

    public JCacheCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager()
    {
        return this.cacheManager;
    }

    public void setAllowNullValues(boolean allowNullValues)
    {
        this.allowNullValues = allowNullValues;
    }

    public boolean isAllowNullValues()
    {
        return this.allowNullValues;
    }

    public void afterPropertiesSet()
    {
        if (getCacheManager() == null) {
            setCacheManager(Caching.getCachingProvider().getCacheManager());
        }
        super.afterPropertiesSet();
    }

    protected Collection<org.springframework.cache.Cache> loadCaches()
    {
        Collection<org.springframework.cache.Cache> caches = new LinkedHashSet();
        for (String cacheName : getCacheManager().getCacheNames())
        {
            javax.cache.Cache<Object, Object> jcache = getCacheManager().getCache(cacheName);
            caches.add(new JCacheCache(jcache, isAllowNullValues()));
        }
        return caches;
    }

    protected org.springframework.cache.Cache getMissingCache(String name)
    {
        javax.cache.Cache<Object, Object> jcache = getCacheManager().getCache(name);
        if (jcache != null) {
            return new JCacheCache(jcache, isAllowNullValues());
        }
        return null;
    }
}
