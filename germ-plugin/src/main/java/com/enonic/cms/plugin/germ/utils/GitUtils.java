package com.enonic.cms.plugin.germ.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by rfo on 19/03/14.
 */
public class GitUtils {
    Logger LOG = LoggerFactory.getLogger(GitUtils.class);

    public GitUtils(){

    }

    public Git getGitInstance(File repositoryFolder) throws IOException{
        return Git.open(repositoryFolder);
    }

    public Git initGitRepository(File repositoryFolder) throws GitAPIException {
        return Git.init().setDirectory(repositoryFolder).call();
    }

    public void reset(File repositoryFolder, String sha1) throws IOException, GitAPIException{
        Repository repository = getRepository(repositoryFolder);
        ResetCommand resetCommand = new Git(repository).reset();
        resetCommand.setMode(ResetCommand.ResetType.HARD);
        resetCommand.setRef(sha1);
        resetCommand.call();

        StoredConfig config = repository.getConfig();
        config.setString("germ", "workspace", repository.getBranch(), sha1);
        config.save();
    }

    public FetchResult fetch(File repositoryFolder, String username, String password)
            throws URISyntaxException, IOException, GitAPIException{

        FetchResult fetchResult;
        Repository repository = getRepository(repositoryFolder);
        String branch = repository.getBranch();
        Config config = repository.getConfig();
        String refSpec = config.getString("remote", "origin", "fetch");

        RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
        RefSpec spec = new RefSpec(refSpec);
        FetchCommand fetchCommand = new Git(repository).fetch()
            .setRemote(remoteConfig.getName())
            .setRefSpecs(spec);

        if (!Strings.isNullOrEmpty(password) && !Strings.isNullOrEmpty(username)){
            UsernamePasswordCredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(username,password);
            fetchResult = fetchCommand
                    .setTimeout(1000)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
        }else{
            fetchResult  = fetchCommand.call();
        }

        return fetchResult;
    }

    public CheckoutResult checkoutBranch(String branch, File repositoryFolder) throws GitAPIException, IOException{
        GitUtils gitUtils = new GitUtils();
        Git git = gitUtils.getGitInstance(repositoryFolder);

        CheckoutCommand checkoutCommand = git.checkout()
                .setName(getBranchSimpleName(branch));
        try{
            checkoutCommand.call();
        }catch (GitAPIException e){
            LOG.error(e.getMessage());
        }
        return checkoutCommand.getResult();
    }

    public CheckoutResult checkoutOrCreateBranch(String branch, File repositoryFolder) throws GitAPIException, IOException{
        GitUtils gitUtils = new GitUtils();
        Git git = gitUtils.getGitInstance(repositoryFolder);
        CreateBranchCommand createBranchCommand = git.branchCreate();
        createBranchCommand
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .setName(getBranchSimpleName(branch))
            .setStartPoint("origin/"+getBranchSimpleName(branch));

        try {
            createBranchCommand.call();
        }catch (RefAlreadyExistsException e){
            LOG.info("Branch already exist, do checkout.");
        }
        CheckoutResult checkoutResult = gitUtils.checkoutBranch(branch,repositoryFolder);
        return checkoutResult;
    }


    public CheckoutResult checkoutFile(String file, String sha1, File repositoryFolder) throws GitAPIException, IOException{
        GitUtils gitUtils = new GitUtils();
        Git git = gitUtils.getGitInstance(repositoryFolder);

        CheckoutCommand checkoutCommand = git.checkout()
                .addPath(file)
                .setStartPoint(sha1);
        try{
            checkoutCommand.call();
        }catch (GitAPIException e){
            LOG.error(e.getMessage());
        }
        return checkoutCommand.getResult();
    }

    public File[] getRepositoryFiles(File repositoryFolder, FilenameFilter filter){
        return repositoryFolder.listFiles(filter);
    }

    public File[] getRepositoryFiles(File repositoryFolder){

        return repositoryFolder.listFiles();
    }

    public RevCommit[] getLocalCommits(File repositoryFolder) throws IOException, GitAPIException, NullPointerException{
        Repository repository = getRepository(repositoryFolder);
        int numberOfCommits = 100;

        Iterable<RevCommit> allLogs = new Git(repository)
                .log()
                .call();

        RevCommit[] allCommits = Iterables.toArray(allLogs, RevCommit.class);

        return allCommits;
    }

    public RevCommit[] getRemoteCommits(File repositoryFolder) throws IOException, GitAPIException, NullPointerException{
        Repository repository = getRepository(repositoryFolder);
        String branch = repository.getBranch();
        int numberOfCommits = 100;

        ObjectId branchObject = repository.resolve("refs/remotes/origin/"+branch);
        LOG.info("branchObject : " + branchObject.getName());

        List<RevCommit> commits = new ArrayList<RevCommit>();
        RevCommit commit = null;
        RevWalk walk = new RevWalk(repository);
        walk.sort(RevSort.NONE);
        RevCommit head = walk.parseCommit(branchObject);
        walk.markStart(head);

        for (int i = 0; i<numberOfCommits;i++){
            commit = walk.next();
            commits.add(commit);
        }
        walk.dispose();

        RevCommit[] allCommits = Iterables.toArray(commits, RevCommit.class);

        return allCommits;
    }

    public void addRemote(File repositoryFolder, String originUrl) throws IOException {
        Repository repository = getRepository(repositoryFolder);
        StoredConfig config = repository.getConfig();
        config.setString("remote", "origin", "url", originUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
    }

    public void removeRemote(File repositoryFolder) throws IOException {
        Repository repository = getRepository(repositoryFolder);
        StoredConfig config = repository.getConfig();
        config.unset("remote", "origin", "url");
        //config.unset("remote", "origin", "fetch");
        config.save();
    }

    public String getRemoteOrigin(File repositoryFolder) throws IOException {
        Repository repository = getRepository(repositoryFolder);
        Config config = repository.getConfig();
        Set<String> remotes = config.getSubsections("remote");
        for (String remote : remotes) {
            if ("origin".equals(remote)) {
                return config.getString("remote", "origin", "url");
            }
        }
        return null;
    }

    public Set<String> clean(File repositoryFolder) throws GitAPIException, IOException{
        Git git = getGitInstance(repositoryFolder);
        return git.clean().setCleanDirectories(true).call();
    }

    public RebaseResult rebase(File repositoryFolder) throws GitAPIException, IOException{
        Git git = getGitInstance(repositoryFolder);
        RebaseCommand rebaseCommand = git.rebase();
        rebaseCommand.setUpstream("HEAD");
        RebaseResult rebaseResult = rebaseCommand.call();
        return rebaseResult;
    }
      
    public Status getStatus(File repositoryFolder) throws IOException, GitAPIException {
        Repository repository = getRepository(repositoryFolder);

        if (!repository.getDirectory().exists()) {
            throw new IOException("No git repository exists.");
        }

        return new Git(repository).status().call();
    }

    public List<Ref> getRemoteBranches(File repositoryFolder) throws Exception{
        Git git = getGitInstance(repositoryFolder);
        ListBranchCommand listBranchCommand = git.branchList();
        List<Ref> remotebranches = listBranchCommand.setListMode(ListBranchCommand.ListMode.REMOTE).call();
        return remotebranches;
    }

    public List<Ref> getLocalBranches(File repositoryFolder) throws Exception{
        Git git = getGitInstance(repositoryFolder);
        ListBranchCommand listBranchCommand = git.branchList();
        List<Ref> localbranches = listBranchCommand.call();
        return localbranches;
    }

    public Repository getRepository(File repositoryFolder) throws IOException {

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(repositoryFolder)
                .readEnvironment()
                .findGitDir()
                .build();
        return repository;
    }

    public String getBranchSimpleName(String branch){
        return StringUtils.substringAfterLast(branch,"/");
    }


}
