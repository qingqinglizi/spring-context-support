package org.springframework.ui.velocity;

import java.io.IOException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

@Deprecated
public class VelocityEngineFactoryBean
        extends VelocityEngineFactory
        implements FactoryBean<VelocityEngine>, InitializingBean, ResourceLoaderAware
{
    private VelocityEngine velocityEngine;

    public void afterPropertiesSet()
            throws IOException, VelocityException
    {
        this.velocityEngine = createVelocityEngine();
    }

    public VelocityEngine getObject()
    {
        return this.velocityEngine;
    }

    public Class<? extends VelocityEngine> getObjectType()
    {
        return VelocityEngine.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
