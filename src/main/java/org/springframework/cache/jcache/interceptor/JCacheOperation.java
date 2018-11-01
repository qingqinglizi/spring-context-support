package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheResolver;

public abstract interface JCacheOperation<A extends Annotation>
        extends BasicOperation, CacheMethodDetails<A>
{
  public abstract CacheResolver getCacheResolver();

  public abstract CacheInvocationParameter[] getAllParameters(Object... paramVarArgs);
}
