package org.springframework.cache.ehcache;

import java.io.IOException;
import java.io.InputStream;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.springframework.core.io.Resource;

public abstract class EhCacheManagerUtils
{
    public static CacheManager buildCacheManager()
            throws CacheException
    {
        return new CacheManager(ConfigurationFactory.parseConfiguration());
    }

    public static CacheManager buildCacheManager(String name)
            throws CacheException
    {
        Configuration configuration = ConfigurationFactory.parseConfiguration();
        configuration.setName(name);
        return new CacheManager(configuration);
    }

    public static CacheManager buildCacheManager(Resource configLocation)
            throws CacheException
    {
        return new CacheManager(parseConfiguration(configLocation));
    }

    public static CacheManager buildCacheManager(String name, Resource configLocation)
            throws CacheException
    {
        Configuration configuration = parseConfiguration(configLocation);
        configuration.setName(name);
        return new CacheManager(configuration);
    }

    public static Configuration parseConfiguration(Resource configLocation)
            throws CacheException
    {
        InputStream is = null;
        try
        {
            is = configLocation.getInputStream();
            return ConfigurationFactory.parseConfiguration(is);
        }
        catch (IOException ex)
        {
            throw new CacheException("Failed to parse EhCache configuration resource", ex);
        }
        finally
        {
            if (is != null) {
                try
                {
                    is.close();
                }
                catch (IOException localIOException2) {}
            }
        }
    }
}
