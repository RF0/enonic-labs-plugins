package com.enonic.cms.plugin.germ;

import com.enonic.cms.api.client.Client;
import com.enonic.cms.api.plugin.PluginConfig;
import com.enonic.cms.api.plugin.PluginEnvironment;
import com.enonic.cms.api.plugin.ext.http.HttpController;
import com.enonic.cms.plugin.germ.utils.GitUtils;
import com.enonic.cms.plugin.germ.utils.Helper;
import com.enonic.cms.plugin.germ.utils.ResponseMessage;
import com.enonic.cms.plugin.germ.view.TemplateEngineProvider;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.WebContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rfo on 07/02/14.
 */


@Component
public class GermPluginController extends HttpController {

    public GermPluginController() throws Exception {
        setDisplayName("G.E.R.M - Git Enonic Release Management");
        setUrlPatterns(new String[]{"/admin/site/[0-9]/germ.*"});
        setPriority(10);

        pluginsFilenameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".jar")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    File folderWithResources;
    File folderWithPlugins;

    File gitFolderWithResources;
    File gitFolderWithPlugins;

    String allowedAdminGroup;
    String allowedUserGroup;

    @Autowired
    Client client;

    @Autowired
    PluginEnvironment pluginEnvironment;

    PluginConfig pluginConfig;

    FilenameFilter pluginsFilenameFilter;

    boolean needsAuthentication = false;

    @Autowired
    public void setPluginConfig(List<PluginConfig> pluginConfig) {
        //TODO: Strange hack with List<PluginConfig> here, srs is investigating
        this.pluginConfig = pluginConfig.get(0);
        folderWithResources = new File(this.pluginConfig.getString("folderWithResources"));
        folderWithPlugins = new File(this.pluginConfig.getString("folderWithPlugins"));
        gitFolderWithResources = new File(folderWithResources + "/.git");
        gitFolderWithPlugins = new File(folderWithPlugins + "/.git");
        this.allowedAdminGroup = this.pluginConfig.getString("allowedAdminGroup");
        this.allowedUserGroup = this.pluginConfig.getString("allowedUserGroup");
    }

    @Autowired
    private ApplicationContext applicationContext;

    //TODO: Check with srs about templateEngineProvider, TemplateEngine is expensive and should only be instanciated once.
    @Autowired
    private TemplateEngineProvider templateEngineProvider;

    Logger LOG = LoggerFactory.getLogger(GermPluginController.class);
    static ConcurrentHashMap<String, List> messages = new ConcurrentHashMap<String, List>();


    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String username = client.getUserName();
        Document userContext = client.getUserContext();
        Helper.prettyPrint(userContext);
        Boolean adminGroupMembership = false;

        if (needsAuthentication) {
            try {
                Element adminEl = ((Element) XPath.selectSingleNode(userContext, "//user/memberships/group[name='" + this.allowedAdminGroup + "']"));
                if (adminEl != null) {
                    adminGroupMembership = new Boolean(adminEl.getAttributeValue("direct-membership"));
                }
            } catch (Exception e) {
                LOG.info("Access denied " + e.getMessage());
            }
            LOG.info("*********************Username************************");
            LOG.info(client.getUserName());
            Helper.prettyPrint(client.getUserContext());
        }


        //Set parameters on context to make them available in Thymeleaf html view
        WebContext context = new WebContext(request, response, request.getSession().getServletContext());

        //Add multiple response messages with addInfoMessage(), addWarningMessage() or addErrorMessage();
        List<ResponseMessage> responseMessages = new ArrayList<ResponseMessage>();
        messages.put(pluginEnvironment.getCurrentSession().getId(), responseMessages);

        String requestPath = StringUtils.substringAfterLast(request.getRequestURI(), "/germ/");
        context.setVariable("requestPath", requestPath);
        context.setVariable("url", StringUtils.substringBeforeLast(request.getRequestURL().toString(), "/germ"));

        if (needsAuthentication && !adminGroupMembership) {
            addErrorMessage("Access denied");
            context.setVariable("accessDenied", true);
        } else {
            addRequestPathContext(requestPath, context);
        }

        response.setContentType("text/html");
        context.setVariable("messages", messages.get(pluginEnvironment.getCurrentSession().getId()));
        messages.remove(pluginEnvironment.getCurrentSession().getId());
        try {
            templateEngineProvider.setApplicationContext(applicationContext);
            templateEngineProvider.get().process("germ", context, response.getWriter());
        } catch (Exception e) {
            addErrorMessage(e.getMessage());
            templateEngineProvider.get().process("errors/404", context, response.getWriter());
        }

    }

    public void runCmd(File folder, File repositoryFolder, WebContext context) {
        GitUtils gitUtils = new GitUtils();
        String cmd = pluginEnvironment.getCurrentRequest().getParameter("cmd");
        try {
            if (!Strings.isNullOrEmpty(cmd)) {
                if ("init".equals(cmd)) {
                    gitUtils.initGitRepository(folder);
                    addInfoMessage("Git repository successfully initiated in " + repositoryFolder);
                } else if ("rmremote".equals(cmd)) {
                    gitUtils.removeRemote(repositoryFolder);
                    addInfoMessage("Remote origin successfully removed");
                } else if ("addremote".equals(cmd)) {
                    String originUrl = pluginEnvironment.getCurrentRequest().getParameter("originUrl");
                    if (Strings.isNullOrEmpty(originUrl)) {
                        addWarningMessage("OriginUrl missing");
                        return;
                    }
                    gitUtils.addRemote(repositoryFolder, originUrl);
                    addInfoMessage("Remote origin " + originUrl + " successfully added");
                } else if ("fetch".equals(cmd)) {
                    String gitusername = pluginEnvironment.getCurrentRequest().getParameter("gitusername");
                    String gitpassword = pluginEnvironment.getCurrentRequest().getParameter("gitpassword");
                    FetchResult fetchResult = gitUtils.fetch(repositoryFolder, gitusername, gitpassword);
                    if (fetchResult != null && fetchResult.getMessages().length() > 0) {
                        addInfoMessage(fetchResult.getMessages());
                    }
                    addInfoMessage("Fetch from remote origin " + fetchResult.getURI() + " was successful.");
                } else if ("checkout".equals(cmd)) {
                    String branch = pluginEnvironment.getCurrentRequest().getParameter("branch");
                    if (Strings.isNullOrEmpty(branch)) {
                        addWarningMessage("'" + branch + "' is not a valid branch for checkout");
                        return;
                    }
                    try {
                        CheckoutResult checkoutResult = gitUtils.checkoutOrCreateBranch(branch, repositoryFolder);
                        context.setVariable("checkoutResult", checkoutResult);
                        addInfoMessage("Successfully checked out " + branch);
                    } catch (Exception e) {
                        addErrorMessage(e.getMessage());
                    }
                } else if ("checkoutfile".equals(cmd)) {
                    String checkoutfile = pluginEnvironment.getCurrentRequest().getParameter("checkoutfile");
                    String checkoutfile_sha1 = pluginEnvironment.getCurrentRequest().getParameter("sha1");
                    if (Strings.isNullOrEmpty(checkoutfile)) {
                        addWarningMessage("'" + checkoutfile + "' is not a valid file for checkout");
                        return;
                    }
                    if (Strings.isNullOrEmpty(checkoutfile_sha1)) {
                        addWarningMessage("'" + checkoutfile_sha1 + "' is not a valid git commit");
                        return;
                    }

                    try {
                        CheckoutResult checkoutResult = gitUtils.checkoutFile(checkoutfile, checkoutfile_sha1, repositoryFolder);
                        context.setVariable("checkoutResult", checkoutResult);
                        addInfoMessage("Successfully checked out " + checkoutfile);
                    } catch (Exception e) {
                        addErrorMessage(e.getMessage());
                    }
                } else if ("rebase".equals(cmd)) {
                    try {
                        RebaseResult rebaseResult = gitUtils.rebase(repositoryFolder);
                        context.setVariable("rebaseResult", rebaseResult);
                    } catch (Exception e) {
                        addErrorMessage(e.getMessage());
                    }
                } else if ("clean".equals(cmd)) {
                    Set<String> cleanedFiles = gitUtils.clean(repositoryFolder);
                    context.setVariable("cleanedFiles", cleanedFiles);
                } else if ("reset".equals(cmd)) {
                    String sha1 = pluginEnvironment.getCurrentRequest().getParameter("sha1");
                    if (Strings.isNullOrEmpty(sha1)) {
                        addWarningMessage("SHA-1 is not set, cannot reset.");
                    }
                    gitUtils.reset(repositoryFolder, sha1);
                    addInfoMessage("Successfully reset to " + sha1);
                }
            }
        } catch (Exception e) {
            addErrorMessage(e.getMessage());
        }
    }

    public void addCommonContext(WebContext context, File repositoryFolder) {
        GitUtils gitUtils = new GitUtils();

        try {
            List<Ref> localbranches = gitUtils.getLocalBranches(repositoryFolder);
            context.setVariable("localbranches", localbranches);
        } catch (Exception e) {
            LOG.info("No local branches: " + e.getMessage());
        }

        try {
            List<Ref> remotebranches = gitUtils.getRemoteBranches(repositoryFolder);
            context.setVariable("remotebranches", remotebranches);
        } catch (Exception e) {
            LOG.info("No remote branches: " + e.getMessage());
        }

        try {
            RevCommit[] localBranchCommits = gitUtils.getLocalCommits(repositoryFolder);
            context.setVariable("localBranchCommits", localBranchCommits);
        } catch (Exception e) {
            LOG.info("getLocalCommits: " + e.getMessage());
        }

        try {
            RevCommit[] remoteBranchCommits = gitUtils.getRemoteCommits(repositoryFolder);
            context.setVariable("remoteBranchCommits", remoteBranchCommits);
        } catch (Exception e) {
            LOG.info("getRemoteCommits: " + e.getMessage());
        }

        try {
            Status status = gitUtils.getStatus(repositoryFolder);
            context.setVariable("status", status);
        } catch (Exception e) {
            LOG.info("No status:" + e.getMessage());
        }

        try {
            context.setVariable("resourcesRepository", gitUtils.getRepository(gitFolderWithResources));
            context.setVariable("resourcesRemoteUrl", gitUtils.getRemoteOrigin(gitFolderWithResources));
            context.setVariable("resourcesRemoteBranches", gitUtils.getRemoteBranches(gitFolderWithResources));
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
        try {
            context.setVariable("pluginsRepository", gitUtils.getRepository(gitFolderWithPlugins));
            context.setVariable("pluginsRemoteUrl", gitUtils.getRemoteOrigin(gitFolderWithPlugins));
            context.setVariable("pluginsRemoteBranches", gitUtils.getRemoteBranches(gitFolderWithPlugins));
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
    }

    public void pluginsStatus(WebContext context) {
        plugins(context);
    }

    public void resourcesStatus(WebContext context) {
        resources(context);
    }

    public void resourcesRemote(WebContext context) {
        resources(context);
    }

    public void pluginsRemote(WebContext context) {
        plugins(context);
    }

    public void pluginsFiles(WebContext context) {
        GitUtils gitUtils = new GitUtils();
        plugins(context);
        context.setVariable("files", gitUtils.getRepositoryFiles(folderWithPlugins));
    }

    public void resourcesFiles(WebContext context) {
        GitUtils gitUtils = new GitUtils();
        resources(context);
        context.setVariable("files", gitUtils.getRepositoryFiles(folderWithResources));
    }


    public void plugins(WebContext context) {
        GitUtils gitUtils = new GitUtils();

        try {
            runCmd(folderWithPlugins, gitFolderWithPlugins, context);
            Repository repository = gitUtils.getRepository(gitFolderWithPlugins);
            addCommonContext(context, gitFolderWithPlugins);
            context.setVariable("repository", gitUtils.getRepository(gitFolderWithPlugins));
            context.setVariable("method", "plugins");
            context.setVariable("directory", folderWithPlugins);
            context.setVariable("gitDirectory", gitFolderWithPlugins);
            context.setVariable("originUrl", gitUtils.getRemoteOrigin(gitFolderWithPlugins));
            context.setVariable("headCommit", repository.getConfig().getString("germ", "workspace", repository.getBranch()));
            context.setVariable("checkoutfiles", gitUtils.getRepositoryFiles(folderWithPlugins, pluginsFilenameFilter));

        } catch (Exception e) {
            addErrorMessage(e.getMessage());
        }
    }

    public void resources(WebContext context) {
        GitUtils gitUtils = new GitUtils();

        try {
            Repository repository = gitUtils.getRepository(gitFolderWithResources);
            runCmd(folderWithResources, gitFolderWithResources, context);
            addCommonContext(context, gitFolderWithResources);

            context.setVariable("repository", gitUtils.getRepository(gitFolderWithResources));
            context.setVariable("method", "resources");
            context.setVariable("directory", folderWithResources);
            context.setVariable("gitDirectory", gitFolderWithResources);
            context.setVariable("originUrl", gitUtils.getRemoteOrigin(gitFolderWithResources));
            context.setVariable("headCommit", repository.getConfig().getString("germ", "workspace", repository.getBranch()));
        } catch (Exception e) {
            addWarningMessage(e.getMessage());
        }


    }

    public void addRequestPathContext(String requestPath, WebContext context) {
        String methodName = resolveMethodNameFromRequestPath(requestPath);
        try {
            Method method = this.getClass().getMethod(methodName, WebContext.class);
            method.invoke(this, context);
        } catch (NoSuchMethodException e) {
            LOG.info("NoSuchMethodException. No method {} defined for requestPath {}. " + e.getMessage(), methodName, requestPath);
        } catch (IllegalAccessException e) {
            LOG.error("IllegalAccessException with reflection " + e.getMessage());
        } catch (InvocationTargetException e) {
            LOG.error("InvocationTargetException with reflection " + e.getMessage());
        }
    }

    public String resolveMethodNameFromRequestPath(String requestPath) {
        String[] words = requestPath.split("/");
        String methodName = "";
        for (String word : words) {
            methodName += StringUtils.capitalize(word.toLowerCase());
        }
        methodName = StringUtils.uncapitalize(methodName);
        LOG.info("Methodname:" + methodName);

        return methodName;
    }


    private void addInfoMessage(String message) {
        addMessage(message, ResponseMessage.MessageType.INFO);
    }

    private void addWarningMessage(String message) {
        addMessage(message, ResponseMessage.MessageType.WARNING);
    }

    private void addErrorMessage(String message) {
        addMessage(message, ResponseMessage.MessageType.ERROR);
    }

    private void addMessage(String message, ResponseMessage.MessageType messageType) {
        if (messageType.compareTo(ResponseMessage.MessageType.ERROR) == 0) {
            LOG.error(message);
        } else if (messageType.compareTo(ResponseMessage.MessageType.WARNING) == 0) {
            LOG.warn(message);
        } else if (messageType.compareTo(ResponseMessage.MessageType.INFO) == 0) {
            LOG.info(message);
        }
        ResponseMessage responseMessage = new ResponseMessage(message, messageType);
        try {
            messages.get(pluginEnvironment.getCurrentSession().getId()).add(responseMessage);
        } catch (Exception e) {
            LOG.error(this.getClass().getSimpleName(), "Error getting messages, and adding response message");
        }

    }

}
