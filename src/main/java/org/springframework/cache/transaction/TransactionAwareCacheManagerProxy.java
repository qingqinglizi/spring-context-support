package org.springframework.cache.transaction;

import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

public class TransactionAwareCacheManagerProxy
        implements CacheManager, InitializingBean
{
    private CacheManager targetCacheManager;

    public TransactionAwareCacheManagerProxy() {}

    public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager)
    {
        Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
        this.targetCacheManager = targetCacheManager;
    }

    public void setTargetCacheManager(CacheManager targetCacheManager)
    {
        this.targetCacheManager = targetCacheManager;
    }

    public void afterPropertiesSet()
    {
        if (this.targetCacheManager == null) {
            throw new IllegalArgumentException("Property 'targetCacheManager' is required");
        }
    }

    public Cache getCache(String name)
    {
        return new TransactionAwareCacheDecorator(this.targetCacheManager.getCache(name));
    }

    public Collection<String> getCacheNames()
    {
        return this.targetCacheManager.getCacheNames();
    }
}
