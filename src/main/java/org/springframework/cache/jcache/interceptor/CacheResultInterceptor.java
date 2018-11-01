package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheResult;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationInvoker.ThrowableWrapper;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.SerializationUtils;

class CacheResultInterceptor
        extends AbstractKeyCacheInterceptor<CacheResultOperation, CacheResult>
{
    public CacheResultInterceptor(CacheErrorHandler errorHandler)
    {
        super(errorHandler);
    }

    protected Object invoke(CacheOperationInvocationContext<CacheResultOperation> context, CacheOperationInvoker invoker)
    {
        CacheResultOperation operation = (CacheResultOperation)context.getOperation();
        Object cacheKey = generateKey(context);

        Cache cache = resolveCache(context);
        Cache exceptionCache = resolveExceptionCache(context);
        if (!operation.isAlwaysInvoked())
        {
            Cache.ValueWrapper cachedValue = doGet(cache, cacheKey);
            if (cachedValue != null) {
                return cachedValue.get();
            }
            checkForCachedException(exceptionCache, cacheKey);
        }
        try
        {
            Object invocationResult = invoker.invoke();
            doPut(cache, cacheKey, invocationResult);
            return invocationResult;
        }
        catch (CacheOperationInvoker.ThrowableWrapper ex)
        {
            Throwable original = ex.getOriginal();
            cacheException(exceptionCache, operation.getExceptionTypeFilter(), cacheKey, original);
            throw ex;
        }
    }

    protected void checkForCachedException(Cache exceptionCache, Object cacheKey)
    {
        if (exceptionCache == null) {
            return;
        }
        Cache.ValueWrapper result = doGet(exceptionCache, cacheKey);
        if (result != null) {
            throw rewriteCallStack((Throwable)result.get(), getClass().getName(), "invoke");
        }
    }

    protected void cacheException(Cache exceptionCache, ExceptionTypeFilter filter, Object cacheKey, Throwable ex)
    {
        if (exceptionCache == null) {
            return;
        }
        if (filter.match(ex.getClass())) {
            doPut(exceptionCache, cacheKey, ex);
        }
    }

    private Cache resolveExceptionCache(CacheOperationInvocationContext<CacheResultOperation> context)
    {
        CacheResolver exceptionCacheResolver = ((CacheResultOperation)context.getOperation()).getExceptionCacheResolver();
        if (exceptionCacheResolver != null) {
            return extractFrom(((CacheResultOperation)context.getOperation()).getExceptionCacheResolver().resolveCaches(context));
        }
        return null;
    }

    private static CacheOperationInvoker.ThrowableWrapper rewriteCallStack(Throwable exception, String className, String methodName)
    {
        Throwable clone = cloneException(exception);
        if (clone == null) {
            return new CacheOperationInvoker.ThrowableWrapper(exception);
        }
        StackTraceElement[] callStack = new Exception().getStackTrace();
        StackTraceElement[] cachedCallStack = exception.getStackTrace();

        int index = findCommonAncestorIndex(callStack, className, methodName);
        int cachedIndex = findCommonAncestorIndex(cachedCallStack, className, methodName);
        if ((index == -1) || (cachedIndex == -1)) {
            return new CacheOperationInvoker.ThrowableWrapper(exception);
        }
        StackTraceElement[] result = new StackTraceElement[cachedIndex + callStack.length - index];
        System.arraycopy(cachedCallStack, 0, result, 0, cachedIndex);
        System.arraycopy(callStack, index, result, cachedIndex, callStack.length - index);

        clone.setStackTrace(result);
        return new CacheOperationInvoker.ThrowableWrapper(clone);
    }

    private static <T extends Throwable> T cloneException(T exception)
    {
        try
        {
            return (Throwable)SerializationUtils.deserialize(SerializationUtils.serialize(exception));
        }
        catch (Exception ex) {}
        return null;
    }

    private static int findCommonAncestorIndex(StackTraceElement[] callStack, String className, String methodName)
    {
        for (int i = 0; i < callStack.length; i++)
        {
            StackTraceElement element = callStack[i];
            if ((className.equals(element.getClassName())) && (methodName.equals(element.getMethodName()))) {
                return i;
            }
        }
        return -1;
    }
}
