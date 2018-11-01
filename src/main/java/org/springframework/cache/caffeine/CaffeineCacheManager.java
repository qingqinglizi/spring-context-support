package org.springframework.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public class CaffeineCacheManager
        implements CacheManager
{
    private final ConcurrentMap<String, org.springframework.cache.Cache> cacheMap = new ConcurrentHashMap(16);
    private boolean dynamic = true;
    private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
    private CacheLoader<Object, Object> cacheLoader;
    private boolean allowNullValues = true;

    public CaffeineCacheManager() {}

    public CaffeineCacheManager(String... cacheNames)
    {
        setCacheNames(Arrays.asList(cacheNames));
    }

    public void setCacheNames(Collection<String> cacheNames)
    {
        if (cacheNames != null)
        {
            for (String name : cacheNames) {
                this.cacheMap.put(name, createCaffeineCache(name));
            }
            this.dynamic = false;
        }
        else
        {
            this.dynamic = true;
        }
    }

    public void setCaffeine(Caffeine<Object, Object> caffeine)
    {
        Assert.notNull(caffeine, "Caffeine must not be null");
        doSetCaffeine(caffeine);
    }

    public void setCaffeineSpec(CaffeineSpec caffeineSpec)
    {
        doSetCaffeine(Caffeine.from(caffeineSpec));
    }

    public void setCacheSpecification(String cacheSpecification)
    {
        doSetCaffeine(Caffeine.from(cacheSpecification));
    }

    public void setCacheLoader(CacheLoader<Object, Object> cacheLoader)
    {
        if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader))
        {
            this.cacheLoader = cacheLoader;
            refreshKnownCaches();
        }
    }

    public void setAllowNullValues(boolean allowNullValues)
    {
        if (this.allowNullValues != allowNullValues)
        {
            this.allowNullValues = allowNullValues;
            refreshKnownCaches();
        }
    }

    public boolean isAllowNullValues()
    {
        return this.allowNullValues;
    }

    public Collection<String> getCacheNames()
    {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }

    public org.springframework.cache.Cache getCache(String name)
    {
        org.springframework.cache.Cache cache = (org.springframework.cache.Cache)this.cacheMap.get(name);
        if ((cache == null) && (this.dynamic)) {
            synchronized (this.cacheMap)
            {
                cache = (org.springframework.cache.Cache)this.cacheMap.get(name);
                if (cache == null)
                {
                    cache = createCaffeineCache(name);
                    this.cacheMap.put(name, cache);
                }
            }
        }
        return cache;
    }

    protected org.springframework.cache.Cache createCaffeineCache(String name)
    {
        return new CaffeineCache(name, createNativeCaffeineCache(name), isAllowNullValues());
    }

    protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name)
    {
        if (this.cacheLoader != null) {
            return this.cacheBuilder.build(this.cacheLoader);
        }
        return this.cacheBuilder.build();
    }

    private void doSetCaffeine(Caffeine<Object, Object> cacheBuilder)
    {
        if (!ObjectUtils.nullSafeEquals(this.cacheBuilder, cacheBuilder))
        {
            this.cacheBuilder = cacheBuilder;
            refreshKnownCaches();
        }
    }

    private void refreshKnownCaches()
    {
        for (Map.Entry<String, org.springframework.cache.Cache> entry : this.cacheMap.entrySet()) {
            entry.setValue(createCaffeineCache((String)entry.getKey()));
        }
    }
}
