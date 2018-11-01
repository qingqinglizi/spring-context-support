package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.CollectionUtils;

abstract class AbstractCacheInterceptor<O extends AbstractJCacheOperation<A>, A extends Annotation>
        extends AbstractCacheInvoker
        implements Serializable
{
    protected final Log logger = LogFactory.getLog(getClass());

    protected AbstractCacheInterceptor(CacheErrorHandler errorHandler)
    {
        super(errorHandler);
    }

    protected abstract Object invoke(CacheOperationInvocationContext<O> paramCacheOperationInvocationContext, CacheOperationInvoker paramCacheOperationInvoker)
            throws Throwable;

    protected Cache resolveCache(CacheOperationInvocationContext<O> context)
    {
        Collection<? extends Cache> caches = ((AbstractJCacheOperation)context.getOperation()).getCacheResolver().resolveCaches(context);
        Cache cache = extractFrom(caches);
        if (cache == null) {
            throw new IllegalStateException("Cache could not have been resolved for " + context.getOperation());
        }
        return cache;
    }

    static Cache extractFrom(Collection<? extends Cache> caches)
    {
        if (CollectionUtils.isEmpty(caches)) {
            return null;
        }
        if (caches.size() == 1) {
            return (Cache)caches.iterator().next();
        }
        throw new IllegalStateException("Unsupported cache resolution result " + caches + ": JSR-107 only supports a single cache.");
    }
}
