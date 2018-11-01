package org.springframework.cache.transaction;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class TransactionAwareCacheDecorator
        implements Cache
{
    private final Cache targetCache;

    public TransactionAwareCacheDecorator(Cache targetCache)
    {
        Assert.notNull(targetCache, "Target Cache must not be null");
        this.targetCache = targetCache;
    }

    public Cache getTargetCache()
    {
        return this.targetCache;
    }

    public String getName()
    {
        return this.targetCache.getName();
    }

    public Object getNativeCache()
    {
        return this.targetCache.getNativeCache();
    }

    public Cache.ValueWrapper get(Object key)
    {
        return this.targetCache.get(key);
    }

    public <T> T get(Object key, Class<T> type)
    {
        return this.targetCache.get(key, type);
    }

    public <T> T get(Object key, Callable<T> valueLoader)
    {
        return this.targetCache.get(key, valueLoader);
    }

    public void put(final Object key, final Object value)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter()
            {
                public void afterCommit()
                {
                    TransactionAwareCacheDecorator.this.targetCache.put(key, value);
                }
            });
        } else {
            this.targetCache.put(key, value);
        }
    }

    public Cache.ValueWrapper putIfAbsent(Object key, Object value)
    {
        return this.targetCache.putIfAbsent(key, value);
    }

    public void evict(final Object key)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter()
            {
                public void afterCommit()
                {
                    TransactionAwareCacheDecorator.this.targetCache.evict(key);
                }
            });
        } else {
            this.targetCache.evict(key);
        }
    }

    public void clear()
    {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter()
            {
                public void afterCommit()
                {
                    TransactionAwareCacheDecorator.this.targetCache.clear();
                }
            });
        } else {
            this.targetCache.clear();
        }
    }
}
