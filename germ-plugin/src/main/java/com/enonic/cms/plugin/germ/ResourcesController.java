package com.enonic.cms.plugin.germ;

import com.enonic.cms.api.plugin.ext.http.HttpController;
import com.enonic.cms.plugin.germ.utils.Helper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class ResourcesController extends HttpController{

    Logger LOG = LoggerFactory.getLogger(ResourcesController.class);
    Map<String,String> supportedContentTypes = ImmutableMap.of("css","text/css","js","text/js","html","text/html","png","image/png");

    public ResourcesController() throws Exception {
        setDisplayName("Plugin Resources Controller");
        setUrlPatterns(new String[]{"/admin/site/[0-9].*_resources.*"});
        setPriority(9);
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String fileName = StringUtils.substringAfterLast(request.getRequestURI(),"/");
        String fileType = StringUtils.substringAfterLast(request.getRequestURI(),".");

        if (Strings.isNullOrEmpty(fileName) || Strings.isNullOrEmpty(fileType)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        LOG.info("Requested file {} in ResourceController", fileName);

        if (supportedContentTypes.containsKey(fileType)){
            response.setContentType(supportedContentTypes.get(fileType));
        }else{
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        Helper.stream(ResourcesController.class.getResourceAsStream(fileName),response.getOutputStream());
    }

}
