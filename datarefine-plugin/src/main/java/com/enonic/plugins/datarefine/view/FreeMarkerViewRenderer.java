package com.enonic.plugins.datarefine.view;

import freemarker.template.Configuration;
import freemarker.template.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * User: rfo
 * Date: 12/14/12
 * Time: 1:15 PM
 */

public class FreeMarkerViewRenderer
        implements ViewRenderer {
    private final Configuration config;

    public FreeMarkerViewRenderer() {
        this.config = new Configuration();
        this.config.setClassForTemplateLoading(getClass(), "/views/");
    }

    public void render(String name, Map<String, Object> model, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        model.put("request", req);
        final Template template = this.config.getTemplate(name);
        template.process(model, res.getWriter());
    }
}
