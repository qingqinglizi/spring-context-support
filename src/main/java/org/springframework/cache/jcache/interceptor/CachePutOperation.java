package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;

class CachePutOperation
        extends AbstractJCacheKeyOperation<CachePut>
{
    private final ExceptionTypeFilter exceptionTypeFilter;
    private final AbstractJCacheOperation.CacheParameterDetail valueParameterDetail;

    public CachePutOperation(CacheMethodDetails<CachePut> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator)
    {
        super(methodDetails, cacheResolver, keyGenerator);
        CachePut ann = (CachePut)methodDetails.getCacheAnnotation();
        this.exceptionTypeFilter = createExceptionTypeFilter(ann.cacheFor(), ann.noCacheFor());
        this.valueParameterDetail = initializeValueParameterDetail(methodDetails.getMethod(), this.allParameterDetails);
        if (this.valueParameterDetail == null) {
            throw new IllegalArgumentException("No parameter annotated with @CacheValue was found for " + methodDetails.getMethod());
        }
    }

    public ExceptionTypeFilter getExceptionTypeFilter()
    {
        return this.exceptionTypeFilter;
    }

    public boolean isEarlyPut()
    {
        return !((CachePut)getCacheAnnotation()).afterInvocation();
    }

    public CacheInvocationParameter getValueParameter(Object... values)
    {
        int parameterPosition = this.valueParameterDetail.getParameterPosition();
        if (parameterPosition >= values.length) {
            throw new IllegalStateException("Values mismatch, value parameter at position " + parameterPosition + " cannot be matched against " + values.length + " value(s)");
        }
        return this.valueParameterDetail.toCacheInvocationParameter(values[parameterPosition]);
    }

    private static AbstractJCacheOperation.CacheParameterDetail initializeValueParameterDetail(Method method, List<AbstractJCacheOperation.CacheParameterDetail> allParameters)
    {
        AbstractJCacheOperation.CacheParameterDetail result = null;
        for (AbstractJCacheOperation.CacheParameterDetail parameter : allParameters) {
            if (parameter.isValue()) {
                if (result == null) {
                    result = parameter;
                } else {
                    throw new IllegalArgumentException("More than one @CacheValue found on " + method + "");
                }
            }
        }
        return result;
    }
}
