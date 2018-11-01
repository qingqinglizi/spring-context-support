package org.springframework.cache.ehcache;

import java.util.Collection;
import java.util.LinkedHashSet;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;

public class EhCacheCacheManager
        extends AbstractTransactionSupportingCacheManager
{
    private CacheManager cacheManager;

    public EhCacheCacheManager() {}

    public EhCacheCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager()
    {
        return this.cacheManager;
    }

    public void afterPropertiesSet()
    {
        if (getCacheManager() == null) {
            setCacheManager(EhCacheManagerUtils.buildCacheManager());
        }
        super.afterPropertiesSet();
    }

    protected Collection<Cache> loadCaches()
    {
        Status status = getCacheManager().getStatus();
        if (!Status.STATUS_ALIVE.equals(status)) {
            throw new IllegalStateException("An 'alive' EhCache CacheManager is required - current cache is " + status.toString());
        }
        String[] names = getCacheManager().getCacheNames();
        Collection<Cache> caches = new LinkedHashSet(names.length);
        for (String name : names) {
            caches.add(new EhCacheCache(getCacheManager().getEhcache(name)));
        }
        return caches;
    }

    protected Cache getMissingCache(String name)
    {
        Ehcache ehcache = getCacheManager().getEhcache(name);
        if (ehcache != null) {
            return new EhCacheCache(ehcache);
        }
        return null;
    }
}
