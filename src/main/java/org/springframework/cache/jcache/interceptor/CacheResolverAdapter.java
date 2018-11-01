package org.springframework.cache.jcache.interceptor;

import java.util.Collection;
import java.util.Collections;
import javax.cache.annotation.CacheInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.util.Assert;

class CacheResolverAdapter
        implements org.springframework.cache.interceptor.CacheResolver
{
    private final javax.cache.annotation.CacheResolver target;

    public CacheResolverAdapter(javax.cache.annotation.CacheResolver target)
    {
        Assert.notNull(target, "JSR-107 CacheResolver is required");
        this.target = target;
    }

    protected javax.cache.annotation.CacheResolver getTarget()
    {
        return this.target;
    }

    public Collection<? extends org.springframework.cache.Cache> resolveCaches(CacheOperationInvocationContext<?> context)
    {
        if (!(context instanceof CacheInvocationContext)) {
            throw new IllegalStateException("Unexpected context " + context);
        }
        CacheInvocationContext<?> cacheInvocationContext = (CacheInvocationContext)context;
        javax.cache.Cache<Object, Object> cache = this.target.resolveCache(cacheInvocationContext);
        if (cache == null) {
            throw new IllegalStateException("Could not resolve cache for " + context + " using " + this.target);
        }
        return Collections.singleton(new JCacheCache(cache));
    }
}
