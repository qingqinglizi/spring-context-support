package org.springframework.cache.ehcache;

import java.util.concurrent.Callable;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

public class EhCacheCache
        implements Cache
{
    private final Ehcache cache;

    public EhCacheCache(Ehcache ehcache)
    {
        Assert.notNull(ehcache, "Ehcache must not be null");
        Status status = ehcache.getStatus();
        if (!Status.STATUS_ALIVE.equals(status)) {
            throw new IllegalArgumentException("An 'alive' Ehcache is required - current cache is " + status.toString());
        }
        this.cache = ehcache;
    }

    public final String getName()
    {
        return this.cache.getName();
    }

    public final Ehcache getNativeCache()
    {
        return this.cache;
    }

    public Cache.ValueWrapper get(Object key)
    {
        Element element = lookup(key);
        return toValueWrapper(element);
    }

    public <T> T get(Object key, Callable<T> valueLoader)
    {
        Element element = lookup(key);
        if (element != null) {
            return element.getObjectValue();
        }
        this.cache.acquireWriteLockOnKey(key);
        try
        {
            element = lookup(key);
            Object localObject1;
            if (element != null) {
                return element.getObjectValue();
            }
            return loadValue(key, valueLoader);
        }
        finally
        {
            this.cache.releaseWriteLockOnKey(key);
        }
    }

    private <T> T loadValue(Object key, Callable<T> valueLoader)
    {
        try
        {
            value = valueLoader.call();
        }
        catch (Throwable ex)
        {
            T value;
            throw new Cache.ValueRetrievalException(key, valueLoader, ex);
        }
        T value;
        put(key, value);
        return value;
    }

    public <T> T get(Object key, Class<T> type)
    {
        Element element = this.cache.get(key);
        Object value = element != null ? element.getObjectValue() : null;
        if ((value != null) && (type != null) && (!type.isInstance(value))) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return value;
    }

    public void put(Object key, Object value)
    {
        this.cache.put(new Element(key, value));
    }

    public Cache.ValueWrapper putIfAbsent(Object key, Object value)
    {
        Element existingElement = this.cache.putIfAbsent(new Element(key, value));
        return toValueWrapper(existingElement);
    }

    public void evict(Object key)
    {
        this.cache.remove(key);
    }

    public void clear()
    {
        this.cache.removeAll();
    }

    private Element lookup(Object key)
    {
        return this.cache.get(key);
    }

    private Cache.ValueWrapper toValueWrapper(Element element)
    {
        return element != null ? new SimpleValueWrapper(element.getObjectValue()) : null;
    }
}
