package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

public abstract class AbstractFallbackJCacheOperationSource
        implements JCacheOperationSource
{
    private static final Object NULL_CACHING_ATTRIBUTE = new Object();
    protected final Log logger = LogFactory.getLog(getClass());
    private final Map<MethodClassKey, Object> cache = new ConcurrentHashMap(1024);

    public JCacheOperation<?> getCacheOperation(Method method, Class<?> targetClass)
    {
        MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
        Object cached = this.cache.get(cacheKey);
        if (cached != null) {
            return cached != NULL_CACHING_ATTRIBUTE ? (JCacheOperation)cached : null;
        }
        JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
        if (operation != null)
        {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
            }
            this.cache.put(cacheKey, operation);
        }
        else
        {
            this.cache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
        }
        return operation;
    }

    private JCacheOperation<?> computeCacheOperation(Method method, Class<?> targetClass)
    {
        if ((allowPublicMethodsOnly()) && (!Modifier.isPublic(method.getModifiers()))) {
            return null;
        }
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);


        JCacheOperation<?> operation = findCacheOperation(specificMethod, targetClass);
        if (operation != null) {
            return operation;
        }
        if (specificMethod != method)
        {
            operation = findCacheOperation(method, targetClass);
            if (operation != null) {
                return operation;
            }
        }
        return null;
    }

    protected abstract JCacheOperation<?> findCacheOperation(Method paramMethod, Class<?> paramClass);

    protected boolean allowPublicMethodsOnly()
    {
        return false;
    }
}
