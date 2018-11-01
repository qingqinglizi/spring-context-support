package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;

class CacheRemoveAllOperation
        extends AbstractJCacheOperation<CacheRemoveAll>
{
    private final ExceptionTypeFilter exceptionTypeFilter;

    public CacheRemoveAllOperation(CacheMethodDetails<CacheRemoveAll> methodDetails, CacheResolver cacheResolver)
    {
        super(methodDetails, cacheResolver);
        CacheRemoveAll ann = (CacheRemoveAll)methodDetails.getCacheAnnotation();
        this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
    }

    public ExceptionTypeFilter getExceptionTypeFilter()
    {
        return this.exceptionTypeFilter;
    }

    public boolean isEarlyRemove()
    {
        return !((CacheRemoveAll)getCacheAnnotation()).afterInvocation();
    }
}
