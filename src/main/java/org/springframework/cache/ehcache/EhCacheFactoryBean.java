package org.springframework.cache.ehcache;

import java.lang.reflect.Method;
import java.util.Set;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public class EhCacheFactoryBean
        extends CacheConfiguration
        implements FactoryBean<Ehcache>, BeanNameAware, InitializingBean
{
    private static final Method setStatisticsEnabledMethod = ClassUtils.getMethodIfAvailable(Ehcache.class, "setStatisticsEnabled", new Class[] { Boolean.TYPE });
    private static final Method setSampledStatisticsEnabledMethod = ClassUtils.getMethodIfAvailable(Ehcache.class, "setSampledStatisticsEnabled", new Class[] { Boolean.TYPE });
    protected final Log logger = LogFactory.getLog(getClass());
    private CacheManager cacheManager;
    private boolean blocking = false;
    private CacheEntryFactory cacheEntryFactory;
    private BootstrapCacheLoader bootstrapCacheLoader;
    private Set<CacheEventListener> cacheEventListeners;
    private boolean statisticsEnabled = false;
    private boolean sampledStatisticsEnabled = false;
    private boolean disabled = false;
    private String beanName;
    private Ehcache cache;

    public EhCacheFactoryBean()
    {
        setMaxEntriesLocalHeap(10000L);
        setMaxElementsOnDisk(10000000);
        setTimeToLiveSeconds(120L);
        setTimeToIdleSeconds(120L);
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setCacheName(String cacheName)
    {
        setName(cacheName);
    }

    public void setTimeToLive(int timeToLive)
    {
        setTimeToLiveSeconds(timeToLive);
    }

    public void setTimeToIdle(int timeToIdle)
    {
        setTimeToIdleSeconds(timeToIdle);
    }

    public void setDiskSpoolBufferSize(int diskSpoolBufferSize)
    {
        setDiskSpoolBufferSizeMB(diskSpoolBufferSize);
    }

    public void setBlocking(boolean blocking)
    {
        this.blocking = blocking;
    }

    public void setCacheEntryFactory(CacheEntryFactory cacheEntryFactory)
    {
        this.cacheEntryFactory = cacheEntryFactory;
    }

    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader)
    {
        this.bootstrapCacheLoader = bootstrapCacheLoader;
    }

    public void setCacheEventListeners(Set<CacheEventListener> cacheEventListeners)
    {
        this.cacheEventListeners = cacheEventListeners;
    }

    public void setStatisticsEnabled(boolean statisticsEnabled)
    {
        this.statisticsEnabled = statisticsEnabled;
    }

    public void setSampledStatisticsEnabled(boolean sampledStatisticsEnabled)
    {
        this.sampledStatisticsEnabled = sampledStatisticsEnabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public void setBeanName(String name)
    {
        this.beanName = name;
    }

    public void afterPropertiesSet()
            throws CacheException
    {
        String cacheName = getName();
        if (cacheName == null)
        {
            cacheName = this.beanName;
            setName(cacheName);
        }
        if (this.cacheManager == null)
        {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Using default EhCache CacheManager for cache region '" + cacheName + "'");
            }
            this.cacheManager = CacheManager.getInstance();
        }
        synchronized (this.cacheManager)
        {
            boolean cacheExists = this.cacheManager.cacheExists(cacheName);
            Ehcache rawCache;
            Ehcache rawCache;
            if (cacheExists)
            {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Using existing EhCache cache region '" + cacheName + "'");
                }
                rawCache = this.cacheManager.getEhcache(cacheName);
            }
            else
            {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Creating new EhCache cache region '" + cacheName + "'");
                }
                rawCache = createCache();
                rawCache.setBootstrapCacheLoader(this.bootstrapCacheLoader);
            }
            if (this.cacheEventListeners != null) {
                for (CacheEventListener listener : this.cacheEventListeners) {
                    rawCache.getCacheEventNotificationService().registerListener(listener);
                }
            }
            if (!cacheExists) {
                this.cacheManager.addCache(rawCache);
            }
            if ((this.statisticsEnabled) && (setStatisticsEnabledMethod != null)) {
                ReflectionUtils.invokeMethod(setStatisticsEnabledMethod, rawCache, new Object[] { Boolean.valueOf(true) });
            }
            if ((this.sampledStatisticsEnabled) && (setSampledStatisticsEnabledMethod != null)) {
                ReflectionUtils.invokeMethod(setSampledStatisticsEnabledMethod, rawCache, new Object[] { Boolean.valueOf(true) });
            }
            if (this.disabled) {
                rawCache.setDisabled(true);
            }
            Ehcache decoratedCache = decorateCache(rawCache);
            if (decoratedCache != rawCache) {
                this.cacheManager.replaceCacheWithDecoratedCache(rawCache, decoratedCache);
            }
            this.cache = decoratedCache;
        }
    }

    protected Cache createCache()
    {
        return new Cache(this);
    }

    protected Ehcache decorateCache(Ehcache cache)
    {
        if (this.cacheEntryFactory != null)
        {
            if ((this.cacheEntryFactory instanceof UpdatingCacheEntryFactory)) {
                return new UpdatingSelfPopulatingCache(cache, (UpdatingCacheEntryFactory)this.cacheEntryFactory);
            }
            return new SelfPopulatingCache(cache, this.cacheEntryFactory);
        }
        if (this.blocking) {
            return new BlockingCache(cache);
        }
        return cache;
    }

    public Ehcache getObject()
    {
        return this.cache;
    }

    public Class<? extends Ehcache> getObjectType()
    {
        if (this.cache != null) {
            return this.cache.getClass();
        }
        if (this.cacheEntryFactory != null)
        {
            if ((this.cacheEntryFactory instanceof UpdatingCacheEntryFactory)) {
                return UpdatingSelfPopulatingCache.class;
            }
            return SelfPopulatingCache.class;
        }
        if (this.blocking) {
            return BlockingCache.class;
        }
        return Cache.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
