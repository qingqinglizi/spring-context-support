package org.springframework.cache.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

public class EhCacheManagerFactoryBean
        implements FactoryBean<CacheManager>, InitializingBean, DisposableBean
{
    protected final Log logger = LogFactory.getLog(getClass());
    private Resource configLocation;
    private String cacheManagerName;
    private boolean acceptExisting = false;
    private boolean shared = false;
    private CacheManager cacheManager;
    private boolean locallyManaged = true;

    public void setConfigLocation(Resource configLocation)
    {
        this.configLocation = configLocation;
    }

    public void setCacheManagerName(String cacheManagerName)
    {
        this.cacheManagerName = cacheManagerName;
    }

    public void setAcceptExisting(boolean acceptExisting)
    {
        this.acceptExisting = acceptExisting;
    }

    public void setShared(boolean shared)
    {
        this.shared = shared;
    }

    public void afterPropertiesSet()
            throws CacheException
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("Initializing EhCache CacheManager" + (this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
        }
        Configuration configuration = this.configLocation != null ? EhCacheManagerUtils.parseConfiguration(this.configLocation) : ConfigurationFactory.parseConfiguration();
        if (this.cacheManagerName != null) {
            configuration.setName(this.cacheManagerName);
        }
        if (this.shared) {
            this.cacheManager = CacheManager.create(configuration);
        } else if (this.acceptExisting) {
            synchronized (CacheManager.class)
            {
                this.cacheManager = CacheManager.getCacheManager(this.cacheManagerName);
                if (this.cacheManager == null) {
                    this.cacheManager = new CacheManager(configuration);
                } else {
                    this.locallyManaged = false;
                }
            }
        } else {
            this.cacheManager = new CacheManager(configuration);
        }
    }

    public CacheManager getObject()
    {
        return this.cacheManager;
    }

    public Class<? extends CacheManager> getObjectType()
    {
        return this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void destroy()
    {
        if (this.locallyManaged)
        {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Shutting down EhCache CacheManager" + (this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
            }
            this.cacheManager.shutdown();
        }
    }
}
