package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;

public abstract interface JCacheOperationSource
{
  public abstract JCacheOperation<?> getCacheOperation(Method paramMethod, Class<?> paramClass);
}
