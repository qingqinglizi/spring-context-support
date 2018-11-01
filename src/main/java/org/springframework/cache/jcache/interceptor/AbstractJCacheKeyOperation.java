package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

abstract class AbstractJCacheKeyOperation<A extends Annotation>
        extends AbstractJCacheOperation<A>
{
    private final KeyGenerator keyGenerator;
    private final List<AbstractJCacheOperation.CacheParameterDetail> keyParameterDetails;

    protected AbstractJCacheKeyOperation(CacheMethodDetails<A> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator)
    {
        super(methodDetails, cacheResolver);
        this.keyGenerator = keyGenerator;
        this.keyParameterDetails = initializeKeyParameterDetails(this.allParameterDetails);
    }

    public KeyGenerator getKeyGenerator()
    {
        return this.keyGenerator;
    }

    public CacheInvocationParameter[] getKeyParameters(Object... values)
    {
        List<CacheInvocationParameter> result = new ArrayList();
        for (AbstractJCacheOperation.CacheParameterDetail keyParameterDetail : this.keyParameterDetails)
        {
            int parameterPosition = keyParameterDetail.getParameterPosition();
            if (parameterPosition >= values.length) {
                throw new IllegalStateException("Values mismatch, key parameter at position " + parameterPosition + " cannot be matched against " + values.length + " value(s)");
            }
            result.add(keyParameterDetail.toCacheInvocationParameter(values[parameterPosition]));
        }
        return (CacheInvocationParameter[])result.toArray(new CacheInvocationParameter[result.size()]);
    }

    private static List<AbstractJCacheOperation.CacheParameterDetail> initializeKeyParameterDetails(List<AbstractJCacheOperation.CacheParameterDetail> allParameters)
    {
        List<AbstractJCacheOperation.CacheParameterDetail> all = new ArrayList();
        List<AbstractJCacheOperation.CacheParameterDetail> annotated = new ArrayList();
        for (AbstractJCacheOperation.CacheParameterDetail allParameter : allParameters)
        {
            if (!allParameter.isValue()) {
                all.add(allParameter);
            }
            if (allParameter.isKey()) {
                annotated.add(allParameter);
            }
        }
        return annotated.isEmpty() ? all : annotated;
    }
}
