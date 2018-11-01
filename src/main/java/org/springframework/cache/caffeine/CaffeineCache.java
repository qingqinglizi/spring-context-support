package org.springframework.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;

@UsesJava8
public class CaffeineCache
        extends AbstractValueAdaptingCache
{
    private final String name;
    private final Cache<Object, Object> cache;

    public CaffeineCache(String name, Cache<Object, Object> cache)
    {
        this(name, cache, true);
    }

    public CaffeineCache(String name, Cache<Object, Object> cache, boolean allowNullValues)
    {
        super(allowNullValues);
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(cache, "Cache must not be null");
        this.name = name;
        this.cache = cache;
    }

    public final String getName()
    {
        return this.name;
    }

    public final Cache<Object, Object> getNativeCache()
    {
        return this.cache;
    }

    public Cache.ValueWrapper get(Object key)
    {
        if ((this.cache instanceof LoadingCache))
        {
            Object value = ((LoadingCache)this.cache).get(key);
            return toValueWrapper(value);
        }
        return super.get(key);
    }

    public <T> T get(Object key, Callable<T> valueLoader)
    {
        return fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
    }

    protected Object lookup(Object key)
    {
        return this.cache.getIfPresent(key);
    }

    public void put(Object key, Object value)
    {
        this.cache.put(key, toStoreValue(value));
    }

    public Cache.ValueWrapper putIfAbsent(Object key, Object value)
    {
        PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
        Object result = this.cache.get(key, callable);
        return callable.called ? null : toValueWrapper(result);
    }

    public void evict(Object key)
    {
        this.cache.invalidate(key);
    }

    public void clear()
    {
        this.cache.invalidateAll();
    }

    private class PutIfAbsentFunction
            implements Function<Object, Object>
    {
        private final Object value;
        private boolean called;

        public PutIfAbsentFunction(Object value)
        {
            this.value = value;
        }

        public Object apply(Object key)
        {
            this.called = true;
            return CaffeineCache.this.toStoreValue(this.value);
        }
    }

    private class LoadFunction
            implements Function<Object, Object>
    {
        private final Callable<?> valueLoader;

        public LoadFunction()
        {
            this.valueLoader = valueLoader;
        }

        public Object apply(Object o)
        {
            try
            {
                return CaffeineCache.this.toStoreValue(this.valueLoader.call());
            }
            catch (Exception ex)
            {
                throw new Cache.ValueRetrievalException(o, this.valueLoader, ex);
            }
        }
    }
}
