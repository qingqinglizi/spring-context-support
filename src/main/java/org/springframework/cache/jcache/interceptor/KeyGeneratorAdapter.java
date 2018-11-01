package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

class KeyGeneratorAdapter
        implements KeyGenerator
{
    private final JCacheOperationSource cacheOperationSource;
    private KeyGenerator keyGenerator;
    private CacheKeyGenerator cacheKeyGenerator;

    public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, KeyGenerator target)
    {
        Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
        Assert.notNull(target, "KeyGenerator must not be null");
        this.cacheOperationSource = cacheOperationSource;
        this.keyGenerator = target;
    }

    public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, CacheKeyGenerator target)
    {
        Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
        Assert.notNull(target, "CacheKeyGenerator must not be null");
        this.cacheOperationSource = cacheOperationSource;
        this.cacheKeyGenerator = target;
    }

    public Object getTarget()
    {
        return this.keyGenerator != null ? this.keyGenerator : this.cacheKeyGenerator;
    }

    public Object generate(Object target, Method method, Object... params)
    {
        JCacheOperation<?> operation = this.cacheOperationSource.getCacheOperation(method, target.getClass());
        if (!AbstractJCacheKeyOperation.class.isInstance(operation)) {
            throw new IllegalStateException("Invalid operation, should be a key-based operation " + operation);
        }
        CacheKeyInvocationContext<?> invocationContext = createCacheKeyInvocationContext(target, operation, params);
        if (this.cacheKeyGenerator != null) {
            return this.cacheKeyGenerator.generateCacheKey(invocationContext);
        }
        return doGenerate(this.keyGenerator, invocationContext);
    }

    private static Object doGenerate(KeyGenerator keyGenerator, CacheKeyInvocationContext<?> context)
    {
        List<Object> parameters = new ArrayList();
        for (CacheInvocationParameter param : context.getKeyParameters())
        {
            Object value = param.getValue();
            if ((param.getParameterPosition() == context.getAllParameters().length - 1) &&
                    (context.getMethod().isVarArgs())) {
                parameters.addAll(CollectionUtils.arrayToList(value));
            } else {
                parameters.add(value);
            }
        }
        return keyGenerator.generate(context.getTarget(), context.getMethod(), parameters.toArray());
    }

    private CacheKeyInvocationContext<?> createCacheKeyInvocationContext(Object target, JCacheOperation<?> operation, Object[] params)
    {
        AbstractJCacheKeyOperation<Annotation> keyCacheOperation = (AbstractJCacheKeyOperation)operation;
        return new DefaultCacheKeyInvocationContext(keyCacheOperation, target, params);
    }
}
