package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationInvoker.ThrowableWrapper;
import org.springframework.util.Assert;

public class JCacheAspectSupport
        extends AbstractCacheInvoker
        implements InitializingBean
{
    protected final Log logger = LogFactory.getLog(getClass());
    private JCacheOperationSource cacheOperationSource;
    private boolean initialized = false;
    private CacheResultInterceptor cacheResultInterceptor;
    private CachePutInterceptor cachePutInterceptor;
    private CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor;
    private CacheRemoveAllInterceptor cacheRemoveAllInterceptor;

    public void setCacheOperationSource(JCacheOperationSource cacheOperationSource)
    {
        Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
        this.cacheOperationSource = cacheOperationSource;
    }

    public JCacheOperationSource getCacheOperationSource()
    {
        return this.cacheOperationSource;
    }

    public void afterPropertiesSet()
    {
        Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSource' property is required: If there are no cacheable methods, then don't use a cache aspect.");

        Assert.state(getErrorHandler() != null, "The 'errorHandler' property is required");

        this.cacheResultInterceptor = new CacheResultInterceptor(getErrorHandler());
        this.cachePutInterceptor = new CachePutInterceptor(getErrorHandler());
        this.cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor(getErrorHandler());
        this.cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor(getErrorHandler());

        this.initialized = true;
    }

    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args)
    {
        if (this.initialized)
        {
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
            JCacheOperation<?> operation = getCacheOperationSource().getCacheOperation(method, targetClass);
            if (operation != null)
            {
                CacheOperationInvocationContext<?> context = createCacheOperationInvocationContext(target, args, operation);
                return execute(context, invoker);
            }
        }
        return invoker.invoke();
    }

    private CacheOperationInvocationContext<?> createCacheOperationInvocationContext(Object target, Object[] args, JCacheOperation<?> operation)
    {
        return new DefaultCacheInvocationContext(operation, target, args);
    }

    private Object execute(CacheOperationInvocationContext<?> context, CacheOperationInvoker invoker)
    {
        CacheOperationInvoker adapter = new CacheOperationInvokerAdapter(invoker);
        BasicOperation operation = context.getOperation();
        if ((operation instanceof CacheResultOperation)) {
            return this.cacheResultInterceptor.invoke(context, adapter);
        }
        if ((operation instanceof CachePutOperation)) {
            return this.cachePutInterceptor.invoke(context, adapter);
        }
        if ((operation instanceof CacheRemoveOperation)) {
            return this.cacheRemoveEntryInterceptor.invoke(context, adapter);
        }
        if ((operation instanceof CacheRemoveAllOperation)) {
            return this.cacheRemoveAllInterceptor.invoke(context, adapter);
        }
        throw new IllegalArgumentException("Cannot handle " + operation);
    }

    protected Object invokeOperation(CacheOperationInvoker invoker)
    {
        return invoker.invoke();
    }

    private class CacheOperationInvokerAdapter
            implements CacheOperationInvoker
    {
        private final CacheOperationInvoker delegate;

        public CacheOperationInvokerAdapter(CacheOperationInvoker delegate)
        {
            this.delegate = delegate;
        }

        public Object invoke()
                throws CacheOperationInvoker.ThrowableWrapper
        {
            return JCacheAspectSupport.this.invokeOperation(this.delegate);
        }
    }
}
