package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.StringUtils;

class CacheResultOperation
        extends AbstractJCacheKeyOperation<CacheResult>
{
    private final ExceptionTypeFilter exceptionTypeFilter;
    private final CacheResolver exceptionCacheResolver;
    private final String exceptionCacheName;

    public CacheResultOperation(CacheMethodDetails<CacheResult> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator, CacheResolver exceptionCacheResolver)
    {
        super(methodDetails, cacheResolver, keyGenerator);
        CacheResult ann = (CacheResult)methodDetails.getCacheAnnotation();
        this.exceptionTypeFilter = createExceptionTypeFilter(ann.cachedExceptions(), ann.nonCachedExceptions());
        this.exceptionCacheResolver = exceptionCacheResolver;
        this.exceptionCacheName = (StringUtils.hasText(ann.exceptionCacheName()) ? ann.exceptionCacheName() : null);
    }

    public ExceptionTypeFilter getExceptionTypeFilter()
    {
        return this.exceptionTypeFilter;
    }

    public boolean isAlwaysInvoked()
    {
        return ((CacheResult)getCacheAnnotation()).skipGet();
    }

    public CacheResolver getExceptionCacheResolver()
    {
        return this.exceptionCacheResolver;
    }

    public String getExceptionCacheName()
    {
        return this.exceptionCacheName;
    }
}
