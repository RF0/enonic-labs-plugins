package com.enonic.cms.plugin.germ.view;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.resourceresolver.ClassLoaderResourceResolver;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.spring3.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import javax.xml.ws.Provider;


@Component
public class TemplateEngineProvider implements Provider<TemplateEngine> {
    org.slf4j.Logger LOG = LoggerFactory.getLogger(TemplateEngineProvider.class);

    private TemplateEngine templateEngine;
    SpringResourceTemplateResolver templateResolver;


    public TemplateEngineProvider() throws Exception{
        LOG.debug("Initializing template Engine");
        templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setPrefix("/views/");
        templateResolver.setSuffix(".html");
        templateResolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }
    public void setApplicationContext(ApplicationContext applicationContext) throws Exception{
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.afterPropertiesSet();
    }


    public TemplateEngine get() {
        return this.templateEngine;
    }

    @Override
    public TemplateEngine invoke(TemplateEngine request) {
        return null;
    }
}