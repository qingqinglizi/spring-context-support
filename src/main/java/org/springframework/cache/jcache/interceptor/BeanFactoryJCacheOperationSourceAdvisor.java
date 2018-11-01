package org.springframework.cache.jcache.interceptor;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

public class BeanFactoryJCacheOperationSourceAdvisor
        extends AbstractBeanFactoryPointcutAdvisor
{
    private JCacheOperationSource cacheOperationSource;
    private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut()
    {
        protected JCacheOperationSource getCacheOperationSource()
        {
            return BeanFactoryJCacheOperationSourceAdvisor.this.cacheOperationSource;
        }
    };

    public void setCacheOperationSource(JCacheOperationSource cacheOperationSource)
    {
        this.cacheOperationSource = cacheOperationSource;
    }

    public void setClassFilter(ClassFilter classFilter)
    {
        this.pointcut.setClassFilter(classFilter);
    }

    public Pointcut getPointcut()
    {
        return this.pointcut;
    }
}
