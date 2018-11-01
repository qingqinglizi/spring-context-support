package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CachePut;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationInvoker.ThrowableWrapper;
import org.springframework.util.ExceptionTypeFilter;

class CachePutInterceptor
        extends AbstractKeyCacheInterceptor<CachePutOperation, CachePut>
{
    public CachePutInterceptor(CacheErrorHandler errorHandler)
    {
        super(errorHandler);
    }

    protected Object invoke(CacheOperationInvocationContext<CachePutOperation> context, CacheOperationInvoker invoker)
    {
        CacheKeyInvocationContext<CachePut> invocationContext = createCacheKeyInvocationContext(context);
        CachePutOperation operation = (CachePutOperation)context.getOperation();

        boolean earlyPut = operation.isEarlyPut();
        Object value = invocationContext.getValueParameter().getValue();
        if (earlyPut) {
            cacheValue(context, value);
        }
        try
        {
            Object result = invoker.invoke();
            if (!earlyPut) {
                cacheValue(context, value);
            }
            return result;
        }
        catch (CacheOperationInvoker.ThrowableWrapper ex)
        {
            Throwable original = ex.getOriginal();
            if ((!earlyPut) && (operation.getExceptionTypeFilter().match(original.getClass()))) {
                cacheValue(context, value);
            }
            throw ex;
        }
    }

    protected void cacheValue(CacheOperationInvocationContext<CachePutOperation> context, Object value)
    {
        Object key = generateKey(context);
        Cache cache = resolveCache(context);
        doPut(cache, key, value);
    }
}
