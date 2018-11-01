package org.springframework.cache.guava;

import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

public class GuavaCache
        extends AbstractValueAdaptingCache
{
    private final String name;
    private final Cache<Object, Object> cache;

    public GuavaCache(String name, Cache<Object, Object> cache)
    {
        this(name, cache, true);
    }

    public GuavaCache(String name, Cache<Object, Object> cache, boolean allowNullValues)
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
        if ((this.cache instanceof LoadingCache)) {
            try
            {
                Object value = ((LoadingCache)this.cache).get(key);
                return toValueWrapper(value);
            }
            catch (ExecutionException ex)
            {
                throw new UncheckedExecutionException(ex.getMessage(), ex);
            }
        }
        return super.get(key);
    }

    public <T> T get(Object key, final Callable<T> valueLoader)
    {
        try
        {
            fromStoreValue(this.cache.get(key, new Callable()
            {
                public Object call()
                        throws Exception
                {
                    return GuavaCache.this.toStoreValue(valueLoader.call());
                }
            }));
        }
        catch (ExecutionException ex)
        {
            throw new Cache.ValueRetrievalException(key, valueLoader, ex.getCause());
        }
        catch (UncheckedExecutionException ex)
        {
            throw new Cache.ValueRetrievalException(key, valueLoader, ex.getCause());
        }
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
        try
        {
            PutIfAbsentCallable callable = new PutIfAbsentCallable(value);
            Object result = this.cache.get(key, callable);
            return callable.called ? null : toValueWrapper(result);
        }
        catch (ExecutionException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    public void evict(Object key)
    {
        this.cache.invalidate(key);
    }

    public void clear()
    {
        this.cache.invalidateAll();
    }

    private class PutIfAbsentCallable
            implements Callable<Object>
    {
        private final Object value;
        private boolean called;

        public PutIfAbsentCallable(Object value)
        {
            this.value = value;
        }

        public Object call()
                throws Exception
        {
            this.called = true;
            return GuavaCache.this.toStoreValue(this.value);
        }
    }
}
