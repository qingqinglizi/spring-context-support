package org.springframework.cache.transaction;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

public abstract class AbstractTransactionSupportingCacheManager
        extends AbstractCacheManager
{
    private boolean transactionAware = false;

    public void setTransactionAware(boolean transactionAware)
    {
        this.transactionAware = transactionAware;
    }

    public boolean isTransactionAware()
    {
        return this.transactionAware;
    }

    protected Cache decorateCache(Cache cache)
    {
        return isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache;
    }
}
