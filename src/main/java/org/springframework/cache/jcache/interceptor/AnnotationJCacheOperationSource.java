package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.StringUtils;

public abstract class AnnotationJCacheOperationSource
        extends AbstractFallbackJCacheOperationSource
{
    protected JCacheOperation<?> findCacheOperation(Method method, Class<?> targetType)
    {
        CacheResult cacheResult = (CacheResult)method.getAnnotation(CacheResult.class);
        CachePut cachePut = (CachePut)method.getAnnotation(CachePut.class);
        CacheRemove cacheRemove = (CacheRemove)method.getAnnotation(CacheRemove.class);
        CacheRemoveAll cacheRemoveAll = (CacheRemoveAll)method.getAnnotation(CacheRemoveAll.class);

        int found = countNonNull(new Object[] { cacheResult, cachePut, cacheRemove, cacheRemoveAll });
        if (found == 0) {
            return null;
        }
        if (found > 1) {
            throw new IllegalStateException("More than one cache annotation found on '" + method + "'");
        }
        CacheDefaults defaults = getCacheDefaults(method, targetType);
        if (cacheResult != null) {
            return createCacheResultOperation(method, defaults, cacheResult);
        }
        if (cachePut != null) {
            return createCachePutOperation(method, defaults, cachePut);
        }
        if (cacheRemove != null) {
            return createCacheRemoveOperation(method, defaults, cacheRemove);
        }
        return createCacheRemoveAllOperation(method, defaults, cacheRemoveAll);
    }

    protected CacheDefaults getCacheDefaults(Method method, Class<?> targetType)
    {
        CacheDefaults annotation = (CacheDefaults)method.getDeclaringClass().getAnnotation(CacheDefaults.class);
        if (annotation != null) {
            return annotation;
        }
        return (CacheDefaults)targetType.getAnnotation(CacheDefaults.class);
    }

    protected CacheResultOperation createCacheResultOperation(Method method, CacheDefaults defaults, CacheResult ann)
    {
        String cacheName = determineCacheName(method, defaults, ann.cacheName());

        CacheResolverFactory cacheResolverFactory = determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
        KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

        CacheMethodDetails<CacheResult> methodDetails = createMethodDetails(method, ann, cacheName);

        org.springframework.cache.interceptor.CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
        org.springframework.cache.interceptor.CacheResolver exceptionCacheResolver = null;
        String exceptionCacheName = ann.exceptionCacheName();
        if (StringUtils.hasText(exceptionCacheName)) {
            exceptionCacheResolver = getExceptionCacheResolver(cacheResolverFactory, methodDetails);
        }
        return new CacheResultOperation(methodDetails, cacheResolver, keyGenerator, exceptionCacheResolver);
    }

    protected CachePutOperation createCachePutOperation(Method method, CacheDefaults defaults, CachePut ann)
    {
        String cacheName = determineCacheName(method, defaults, ann.cacheName());

        CacheResolverFactory cacheResolverFactory = determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
        KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

        CacheMethodDetails<CachePut> methodDetails = createMethodDetails(method, ann, cacheName);
        org.springframework.cache.interceptor.CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
        return new CachePutOperation(methodDetails, cacheResolver, keyGenerator);
    }

    protected CacheRemoveOperation createCacheRemoveOperation(Method method, CacheDefaults defaults, CacheRemove ann)
    {
        String cacheName = determineCacheName(method, defaults, ann.cacheName());

        CacheResolverFactory cacheResolverFactory = determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
        KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

        CacheMethodDetails<CacheRemove> methodDetails = createMethodDetails(method, ann, cacheName);
        org.springframework.cache.interceptor.CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
        return new CacheRemoveOperation(methodDetails, cacheResolver, keyGenerator);
    }

    protected CacheRemoveAllOperation createCacheRemoveAllOperation(Method method, CacheDefaults defaults, CacheRemoveAll ann)
    {
        String cacheName = determineCacheName(method, defaults, ann.cacheName());

        CacheResolverFactory cacheResolverFactory = determineCacheResolverFactory(defaults, ann.cacheResolverFactory());

        CacheMethodDetails<CacheRemoveAll> methodDetails = createMethodDetails(method, ann, cacheName);
        org.springframework.cache.interceptor.CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
        return new CacheRemoveAllOperation(methodDetails, cacheResolver);
    }

    private <A extends Annotation> CacheMethodDetails<A> createMethodDetails(Method method, A annotation, String cacheName)
    {
        return new DefaultCacheMethodDetails(method, annotation, cacheName);
    }

    protected org.springframework.cache.interceptor.CacheResolver getCacheResolver(CacheResolverFactory factory, CacheMethodDetails<?> details)
    {
        if (factory != null)
        {
            javax.cache.annotation.CacheResolver cacheResolver = factory.getCacheResolver(details);
            return new CacheResolverAdapter(cacheResolver);
        }
        return getDefaultCacheResolver();
    }

    protected org.springframework.cache.interceptor.CacheResolver getExceptionCacheResolver(CacheResolverFactory factory, CacheMethodDetails<CacheResult> details)
    {
        if (factory != null)
        {
            javax.cache.annotation.CacheResolver cacheResolver = factory.getExceptionCacheResolver(details);
            return new CacheResolverAdapter(cacheResolver);
        }
        return getDefaultExceptionCacheResolver();
    }

    protected CacheResolverFactory determineCacheResolverFactory(CacheDefaults defaults, Class<? extends CacheResolverFactory> candidate)
    {
        if (CacheResolverFactory.class != candidate) {
            return (CacheResolverFactory)getBean(candidate);
        }
        if ((defaults != null) && (CacheResolverFactory.class != defaults.cacheResolverFactory())) {
            return (CacheResolverFactory)getBean(defaults.cacheResolverFactory());
        }
        return null;
    }

    protected KeyGenerator determineKeyGenerator(CacheDefaults defaults, Class<? extends CacheKeyGenerator> candidate)
    {
        if (CacheKeyGenerator.class != candidate) {
            return new KeyGeneratorAdapter(this, (CacheKeyGenerator)getBean(candidate));
        }
        if ((defaults != null) && (CacheKeyGenerator.class != defaults.cacheKeyGenerator())) {
            return new KeyGeneratorAdapter(this, (CacheKeyGenerator)getBean(defaults.cacheKeyGenerator()));
        }
        return getDefaultKeyGenerator();
    }

    protected String determineCacheName(Method method, CacheDefaults defaults, String candidate)
    {
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        if ((defaults != null) && (StringUtils.hasText(defaults.cacheName()))) {
            return defaults.cacheName();
        }
        return generateDefaultCacheName(method);
    }

    protected String generateDefaultCacheName(Method method)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameters = new ArrayList(parameterTypes.length);
        for (Class<?> parameterType : parameterTypes) {
            parameters.add(parameterType.getName());
        }
        StringBuilder sb = new StringBuilder(method.getDeclaringClass().getName());
        sb.append(".").append(method.getName());
        sb.append("(").append(StringUtils.collectionToCommaDelimitedString(parameters)).append(")");
        return sb.toString();
    }

    private int countNonNull(Object... instances)
    {
        int result = 0;
        for (Object instance : instances) {
            if (instance != null) {
                result++;
            }
        }
        return result;
    }

    protected abstract <T> T getBean(Class<T> paramClass);

    protected abstract org.springframework.cache.interceptor.CacheResolver getDefaultCacheResolver();

    protected abstract org.springframework.cache.interceptor.CacheResolver getDefaultExceptionCacheResolver();

    protected abstract KeyGenerator getDefaultKeyGenerator();
}
