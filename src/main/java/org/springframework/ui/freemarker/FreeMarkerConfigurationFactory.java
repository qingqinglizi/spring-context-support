package org.springframework.ui.freemarker;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

public class FreeMarkerConfigurationFactory
{
    protected final Log logger = LogFactory.getLog(getClass());
    private Resource configLocation;
    private Properties freemarkerSettings;
    private Map<String, Object> freemarkerVariables;
    private String defaultEncoding;
    private final List<TemplateLoader> templateLoaders = new ArrayList();
    private List<TemplateLoader> preTemplateLoaders;
    private List<TemplateLoader> postTemplateLoaders;
    private String[] templateLoaderPaths;
    private ResourceLoader resourceLoader = new DefaultResourceLoader();
    private boolean preferFileSystemAccess = true;

    public void setConfigLocation(Resource resource)
    {
        this.configLocation = resource;
    }

    public void setFreemarkerSettings(Properties settings)
    {
        this.freemarkerSettings = settings;
    }

    public void setFreemarkerVariables(Map<String, Object> variables)
    {
        this.freemarkerVariables = variables;
    }

    public void setDefaultEncoding(String defaultEncoding)
    {
        this.defaultEncoding = defaultEncoding;
    }

    public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders)
    {
        this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
    }

    public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders)
    {
        this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
    }

    public void setTemplateLoaderPath(String templateLoaderPath)
    {
        this.templateLoaderPaths = new String[] { templateLoaderPath };
    }

    public void setTemplateLoaderPaths(String... templateLoaderPaths)
    {
        this.templateLoaderPaths = templateLoaderPaths;
    }

    public void setResourceLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    protected ResourceLoader getResourceLoader()
    {
        return this.resourceLoader;
    }

    public void setPreferFileSystemAccess(boolean preferFileSystemAccess)
    {
        this.preferFileSystemAccess = preferFileSystemAccess;
    }

    protected boolean isPreferFileSystemAccess()
    {
        return this.preferFileSystemAccess;
    }

    public Configuration createConfiguration()
            throws IOException, TemplateException
    {
        Configuration config = newConfiguration();
        Properties props = new Properties();
        if (this.configLocation != null)
        {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Loading FreeMarker configuration from " + this.configLocation);
            }
            PropertiesLoaderUtils.fillProperties(props, this.configLocation);
        }
        if (this.freemarkerSettings != null) {
            props.putAll(this.freemarkerSettings);
        }
        if (!props.isEmpty()) {
            config.setSettings(props);
        }
        if (!CollectionUtils.isEmpty(this.freemarkerVariables)) {
            config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
        }
        if (this.defaultEncoding != null) {
            config.setDefaultEncoding(this.defaultEncoding);
        }
        List<TemplateLoader> templateLoaders = new LinkedList(this.templateLoaders);
        if (this.preTemplateLoaders != null) {
            templateLoaders.addAll(this.preTemplateLoaders);
        }
        if (this.templateLoaderPaths != null) {
            for (String path : this.templateLoaderPaths) {
                templateLoaders.add(getTemplateLoaderForPath(path));
            }
        }
        postProcessTemplateLoaders(templateLoaders);
        if (this.postTemplateLoaders != null) {
            templateLoaders.addAll(this.postTemplateLoaders);
        }
        TemplateLoader loader = getAggregateTemplateLoader(templateLoaders);
        if (loader != null) {
            config.setTemplateLoader(loader);
        }
        postProcessConfiguration(config);
        return config;
    }

    protected Configuration newConfiguration()
            throws IOException, TemplateException
    {
        return new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }

    protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath)
    {
        if (isPreferFileSystemAccess()) {
            try
            {
                Resource path = getResourceLoader().getResource(templateLoaderPath);
                File file = path.getFile();
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Template loader path [" + path + "] resolved to file path [" + file
                            .getAbsolutePath() + "]");
                }
                return new FileTemplateLoader(file);
            }
            catch (Exception ex)
            {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Cannot resolve template loader path [" + templateLoaderPath + "] to [java.io.File]: using SpringTemplateLoader as fallback", ex);
                }
                return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
            }
        }
        this.logger.debug("File system access not preferred: using SpringTemplateLoader");
        return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
    }

    protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {}

    protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders)
    {
        int loaderCount = templateLoaders.size();
        switch (loaderCount)
        {
            case 0:
                this.logger.info("No FreeMarker TemplateLoaders specified");
                return null;
            case 1:
                return (TemplateLoader)templateLoaders.get(0);
        }
        TemplateLoader[] loaders = (TemplateLoader[])templateLoaders.toArray(new TemplateLoader[loaderCount]);
        return new MultiTemplateLoader(loaders);
    }

    protected void postProcessConfiguration(Configuration config)
            throws IOException, TemplateException
    {}
}
