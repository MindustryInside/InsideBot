package inside.scheduler;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.context.*;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class AnnotationAwareSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware{

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException{
        this.context = context;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception{
        Object jobInstance = super.createJobInstance(bundle);
        context.getAutowireCapableBeanFactory().autowireBean(jobInstance);
        return jobInstance;
    }
}
