package com.enonic.cms.plugin.datarefine.view;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ViewRenderer
{
    public void render(String name, Map<String, Object> model, HttpServletRequest req, HttpServletResponse res)
            throws Exception;
}