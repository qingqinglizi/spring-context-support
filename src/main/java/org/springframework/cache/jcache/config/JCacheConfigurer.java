package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;

public abstract interface JCacheConfigurer
        extends CachingConfigurer
{
  public abstract CacheResolver exceptionCacheResolver();
}
