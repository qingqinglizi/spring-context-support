package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheValue;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.Assert;
import org.springframework.util.ExceptionTypeFilter;

abstract class AbstractJCacheOperation<A extends Annotation>
        implements JCacheOperation<A>
{
    private final CacheMethodDetails<A> methodDetails;
    private final CacheResolver cacheResolver;
    protected final List<CacheParameterDetail> allParameterDetails;

    protected AbstractJCacheOperation(CacheMethodDetails<A> methodDetails, CacheResolver cacheResolver)
    {
        Assert.notNull(methodDetails, "method details must not be null.");
        Assert.notNull(cacheResolver, "cache resolver must not be null.");
        this.methodDetails = methodDetails;
        this.cacheResolver = cacheResolver;
        this.allParameterDetails = initializeAllParameterDetails(methodDetails.getMethod());
    }

    public abstract ExceptionTypeFilter getExceptionTypeFilter();

    public Method getMethod()
    {
        return this.methodDetails.getMethod();
    }

    public Set<Annotation> getAnnotations()
    {
        return this.methodDetails.getAnnotations();
    }

    public A getCacheAnnotation()
    {
        return this.methodDetails.getCacheAnnotation();
    }

    public String getCacheName()
    {
        return this.methodDetails.getCacheName();
    }

    public Set<String> getCacheNames()
    {
        return Collections.singleton(getCacheName());
    }

    public CacheResolver getCacheResolver()
    {
        return this.cacheResolver;
    }

    public CacheInvocationParameter[] getAllParameters(Object... values)
    {
        if (this.allParameterDetails.size() != values.length) {
            throw new IllegalStateException("Values mismatch, operation has " + this.allParameterDetails.size() + " parameter(s) but got " + values.length + " value(s)");
        }
        List<CacheInvocationParameter> result = new ArrayList();
        for (int i = 0; i < this.allParameterDetails.size(); i++) {
            result.add(((CacheParameterDetail)this.allParameterDetails.get(i)).toCacheInvocationParameter(values[i]));
        }
        return (CacheInvocationParameter[])result.toArray(new CacheInvocationParameter[result.size()]);
    }

    protected ExceptionTypeFilter createExceptionTypeFilter(Class<? extends Throwable>[] includes, Class<? extends Throwable>[] excludes)
    {
        return new ExceptionTypeFilter(Arrays.asList(includes), Arrays.asList(excludes), true);
    }

    public String toString()
    {
        return "]";
    }

    protected StringBuilder getOperationDescription()
    {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append("[");
        result.append(this.methodDetails);
        return result;
    }

    private static List<CacheParameterDetail> initializeAllParameterDetails(Method method)
    {
        List<CacheParameterDetail> result = new ArrayList();
        for (int i = 0; i < method.getParameterTypes().length; i++)
        {
            CacheParameterDetail detail = new CacheParameterDetail(method, i);
            result.add(detail);
        }
        return result;
    }

    protected static class CacheParameterDetail
    {
        private final Class<?> rawType;
        private final Set<Annotation> annotations;
        private final int parameterPosition;
        private final boolean isKey;
        private final boolean isValue;

        public CacheParameterDetail(Method method, int parameterPosition)
        {
            this.rawType = method.getParameterTypes()[parameterPosition];
            this.annotations = new LinkedHashSet();
            boolean foundKeyAnnotation = false;
            boolean foundValueAnnotation = false;
            for (Annotation annotation : method.getParameterAnnotations()[parameterPosition])
            {
                this.annotations.add(annotation);
                if (CacheKey.class.isAssignableFrom(annotation.annotationType())) {
                    foundKeyAnnotation = true;
                }
                if (CacheValue.class.isAssignableFrom(annotation.annotationType())) {
                    foundValueAnnotation = true;
                }
            }
            this.parameterPosition = parameterPosition;
            this.isKey = foundKeyAnnotation;
            this.isValue = foundValueAnnotation;
        }

        public int getParameterPosition()
        {
            return this.parameterPosition;
        }

        protected boolean isKey()
        {
            return this.isKey;
        }

        protected boolean isValue()
        {
            return this.isValue;
        }

        public CacheInvocationParameter toCacheInvocationParameter(Object value)
        {
            return new AbstractJCacheOperation.CacheInvocationParameterImpl(this, value);
        }
    }

    protected static class CacheInvocationParameterImpl
            implements CacheInvocationParameter
    {
        private final AbstractJCacheOperation.CacheParameterDetail detail;
        private final Object value;

        public CacheInvocationParameterImpl(AbstractJCacheOperation.CacheParameterDetail detail, Object value)
        {
            this.detail = detail;
            this.value = value;
        }

        public Class<?> getRawType()
        {
            return AbstractJCacheOperation.CacheParameterDetail.access$000(this.detail);
        }

        public Object getValue()
        {
            return this.value;
        }

        public Set<Annotation> getAnnotations()
        {
            return AbstractJCacheOperation.CacheParameterDetail.access$100(this.detail);
        }

        public int getParameterPosition()
        {
            return AbstractJCacheOperation.CacheParameterDetail.access$200(this.detail);
        }
    }
}
