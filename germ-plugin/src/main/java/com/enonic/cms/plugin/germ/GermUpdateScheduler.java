package com.enonic.cms.plugin.germ;

import com.enonic.cms.api.plugin.PluginConfig;
import com.enonic.cms.api.plugin.ext.TaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Properties;

@Component
public class GermUpdateScheduler extends TaskHandler{

    PluginConfig pluginConfig;

    @Autowired
    public void setPluginConfig(List<PluginConfig> pluginConfig) {
        //TODO: Strange hack with List<PluginConfig> here, srs is investigating
        this.pluginConfig = pluginConfig.get(0);
        folderWithResources = new File(this.pluginConfig.getString("folderWithResources"));
        gitFolderWithResources = new File(folderWithResources + "/.git");
    }

    File folderWithResources;
    File gitFolderWithResources;

    Logger LOG = LoggerFactory.getLogger(GermUpdateScheduler.class);

    @Override
    public void execute(Properties properties) throws Exception {
        LOG.info("GermUpdateScheduler");
        LOG.info("Folder with resources: " + folderWithResources);
        LOG.info("Git folder with resources: " + gitFolderWithResources);
    }
}
