package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;

class CacheRemoveOperation
        extends AbstractJCacheKeyOperation<CacheRemove>
{
    private final ExceptionTypeFilter exceptionTypeFilter;

    public CacheRemoveOperation(CacheMethodDetails<CacheRemove> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator)
    {
        super(methodDetails, cacheResolver, keyGenerator);
        CacheRemove ann = (CacheRemove)methodDetails.getCacheAnnotation();
        this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
    }

    public ExceptionTypeFilter getExceptionTypeFilter()
    {
        return this.exceptionTypeFilter;
    }

    public boolean isEarlyRemove()
    {
        return !((CacheRemove)getCacheAnnotation()).afterInvocation();
    }
}
