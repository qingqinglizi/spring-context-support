package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheRemoveAll;
import org.apache.commons.logging.Log;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationInvoker.ThrowableWrapper;
import org.springframework.util.ExceptionTypeFilter;

class CacheRemoveAllInterceptor
        extends AbstractCacheInterceptor<CacheRemoveAllOperation, CacheRemoveAll>
{
    protected CacheRemoveAllInterceptor(CacheErrorHandler errorHandler)
    {
        super(errorHandler);
    }

    protected Object invoke(CacheOperationInvocationContext<CacheRemoveAllOperation> context, CacheOperationInvoker invoker)
    {
        CacheRemoveAllOperation operation = (CacheRemoveAllOperation)context.getOperation();

        boolean earlyRemove = operation.isEarlyRemove();
        if (earlyRemove) {
            removeAll(context);
        }
        try
        {
            Object result = invoker.invoke();
            if (!earlyRemove) {
                removeAll(context);
            }
            return result;
        }
        catch (CacheOperationInvoker.ThrowableWrapper ex)
        {
            Throwable original = ex.getOriginal();
            if ((!earlyRemove) && (operation.getExceptionTypeFilter().match(original.getClass()))) {
                removeAll(context);
            }
            throw ex;
        }
    }

    protected void removeAll(CacheOperationInvocationContext<CacheRemoveAllOperation> context)
    {
        Cache cache = resolveCache(context);
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Invalidating entire cache '" + cache.getName() + "' for operation " + context
                    .getOperation());
        }
        doClear(cache);
    }
}
