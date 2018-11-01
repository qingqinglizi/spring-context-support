package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

public abstract class JCacheOperationSourcePointcut
        extends StaticMethodMatcherPointcut
        implements Serializable
{
    public boolean matches(Method method, Class<?> targetClass)
    {
        JCacheOperationSource cas = getCacheOperationSource();
        return (cas != null) && (cas.getCacheOperation(method, targetClass) != null);
    }

    protected abstract JCacheOperationSource getCacheOperationSource();

    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JCacheOperationSourcePointcut)) {
            return false;
        }
        JCacheOperationSourcePointcut otherPc = (JCacheOperationSourcePointcut)other;
        return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
    }

    public int hashCode()
    {
        return JCacheOperationSourcePointcut.class.hashCode();
    }

    public String toString()
    {
        return getClass().getName() + ": " + getCacheOperationSource();
    }
}
