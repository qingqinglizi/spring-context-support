package org.springframework.cache.jcache;

import java.util.concurrent.Callable;
import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

public class JCacheCache
        extends AbstractValueAdaptingCache
{
    private final Cache<Object, Object> cache;

    public JCacheCache(Cache<Object, Object> jcache)
    {
        this(jcache, true);
    }

    public JCacheCache(Cache<Object, Object> jcache, boolean allowNullValues)
    {
        super(allowNullValues);
        Assert.notNull(jcache, "Cache must not be null");
        this.cache = jcache;
    }

    public final String getName()
    {
        return this.cache.getName();
    }

    public final Cache<Object, Object> getNativeCache()
    {
        return this.cache;
    }

    protected Object lookup(Object key)
    {
        return this.cache.get(key);
    }

    public <T> T get(Object key, Callable<T> valueLoader)
    {
        try
        {
            return this.cache.invoke(key, new ValueLoaderEntryProcessor(null), new Object[] { valueLoader });
        }
        catch (EntryProcessorException ex)
        {
            throw new Cache.ValueRetrievalException(key, valueLoader, ex.getCause());
        }
    }

    public void put(Object key, Object value)
    {
        this.cache.put(key, toStoreValue(value));
    }

    public Cache.ValueWrapper putIfAbsent(Object key, Object value)
    {
        boolean set = this.cache.putIfAbsent(key, toStoreValue(value));
        return set ? null : get(key);
    }

    public void evict(Object key)
    {
        this.cache.remove(key);
    }

    public void clear()
    {
        this.cache.removeAll();
    }

    private class ValueLoaderEntryProcessor<T>
            implements EntryProcessor<Object, Object, T>
    {
        private ValueLoaderEntryProcessor() {}

        public T process(MutableEntry<Object, Object> entry, Object... arguments)
                throws EntryProcessorException
        {
            Callable<T> valueLoader = (Callable)arguments[0];
            if (entry.exists()) {
                return JCacheCache.this.fromStoreValue(entry.getValue());
            }
            try
            {
                value = valueLoader.call();
            }
            catch (Exception ex)
            {
                T value;
                throw new EntryProcessorException("Value loader '" + valueLoader + "' failed to compute  value for key '" + entry.getKey() + "'", ex);
            }
            T value;
            entry.setValue(JCacheCache.this.toStoreValue(value));
            return value;
        }
    }
}
