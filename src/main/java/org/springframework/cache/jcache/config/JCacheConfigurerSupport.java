package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;

public class JCacheConfigurerSupport
        extends CachingConfigurerSupport
        implements JCacheConfigurer
{
    public CacheResolver exceptionCacheResolver()
    {
        return null;
    }
}
