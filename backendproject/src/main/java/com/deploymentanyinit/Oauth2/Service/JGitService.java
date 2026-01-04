package com.deploymentanyinit.Oauth2.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class JGitService {
    private final OAuth2AuthorizedClientService authorizedClientService;
    private static final Logger logger = LoggerFactory.getLogger(JGitService.class);

    @Autowired
    public JGitService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }
    /////////////////////clone///////////////////
    public void cloneRepository(String remoteUrl, Path localPath, String branch, OAuth2AuthenticationToken authToken)
            throws GitAPIException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName());

        String accessToken = client.getAccessToken().getTokenValue();

        Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localPath.toFile())
                .setBranch(branch)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", accessToken))
                .call();
    }

    private void configureRemote(Repository repo, String remoteUrl) throws IOException {
        StoredConfig config = repo.getConfig();

        config.setString("remote", "origin", "url", remoteUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
        logger.info("Remote configuré pour : {}", remoteUrl);
    }
    public void commitAndPush(Path repoPath, String message, String branch, String remoteUrl, OAuth2AuthenticationToken authToken)
            throws IOException, GitAPIException {

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Invalid GitHub authentication");
        }

        String token = client.getAccessToken().getTokenValue();
        Set<String> scopes = client.getAccessToken().getScopes();
        logger.info("Valeur du jeton : {}", token);
        logger.info("Scopes du jeton : {}", scopes);



        File gitDir = repoPath.resolve(".git").toFile();
        if (!gitDir.exists() || !gitDir.isDirectory() || gitDir.list().length <= 1) {
            logger.warn(".git inexistant ou incomplet. Exécution de git init...");
            try (Git initGit = Git.init().setDirectory(repoPath.toFile()).call()) {
                logger.info("Dépôt Git initialisé.");
            }
        }

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(repoPath.resolve(".git").toFile())
                .build();
             Git git = new Git(repo)) {

            // Configure remote
            configureRemote(repo, remoteUrl);

            // Stage all changes
            git.add().addFilepattern(".").call();

            // Commit changes
            git.commit()
                    .setMessage(message)
                    .call();

            // Push to the SPECIFIED BRANCH (no fallback)
            git.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(branch + ":" + branch))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
                    .call();

            logger.info("Successfully pushed to branch: {}", branch);

        } catch (TransportException e) {
            logger.error("Push failed to branch: {}", branch, e);
            throw new TransportException("Failed to push to " + branch + ": " + e.getMessage(), e);
        }
    }


/////////////////////clone///////////////////

private String extractRepoName(String url) {
    return url.replaceAll(".*[/:]([^/]+?)(\\.git)?$", "$1");
}
public Path cloneToDesktop(String repoUrl, String branch, OAuth2AuthenticationToken authToken)
        throws GitAPIException, IOException {

    Path desktopPath = Paths.get(
            System.getProperty("user.home"),
            "Desktop",
            this.extractRepoName(repoUrl) // Appel via this
    );

    if (!Files.exists(desktopPath)) {
        Files.createDirectories(desktopPath);
    }

    this.cloneRepository(repoUrl, desktopPath, branch, authToken);

    return desktopPath;
}
/////////////////pull//////////////////
public Map<String, Object> pullRepository(Path repoPath, String branch, OAuth2AuthenticationToken authToken, String remoteUrl) {
    try {
        // Vérification de l'authentification
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Authentification GitHub invalide");
        }

        // Vérification du dépôt local
        Path gitDir = repoPath.resolve(".git");
        if (!Files.exists(gitDir)) {
            throw new IllegalArgumentException("Le chemin spécifié n'est pas un dépôt Git valide");
        }

        String token = client.getAccessToken().getTokenValue();

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(gitDir.toFile())
                .build();
             Git git = new Git(repo)) {

            // 1. Obtenir l'URL du remote existant
            StoredConfig config = repo.getConfig();
            remoteUrl = config.getString("remote", "origin", "url");

            // 2. Si l'URL existe mais est incorrecte, ou si elle n'existe pas
            if (remoteUrl == null || !isValidGitHubUrl(remoteUrl)) {
                // Obtenez l'URL correcte depuis le frontend ou une autre source
                remoteUrl = getCorrectRemoteUrl(repoPath, authToken);

                if (remoteUrl == null) {
                    throw new IllegalStateException("Impossible de déterminer l'URL correcte du dépôt GitHub");
                }

                // Configuration du remote
                config.setString("remote", "origin", "url", remoteUrl);
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                config.save();
                logger.info("Remote 'origin' configuré avec l'URL: {}", remoteUrl);
            }

            // 3. Vérification que le remote est accessible
            try {
                git.lsRemote()
                        .setRemote("origin")
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
                        .call();
            } catch (Exception e) {
                throw new IllegalStateException("Impossible d'accéder au remote 'origin'. URL: " + remoteUrl +
                        ". Erreur: " + e.getMessage());
            }

            // 4. Exécution du pull
            PullResult pullResult = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branch)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
                    .call();

            return Map.of(
                    "success", true,
                    "message", "Pull réussi",
                    "status", pullResult.getMergeResult().getMergeStatus().toString()
            );
        }
    } catch (Exception e) {
        logger.error("Échec du pull", e);
        return Map.of(
                "success", false,
                "error", "Échec du pull: " + e.getMessage(),
                "exception", e.getClass().getSimpleName()
        );
    }
}

    private boolean isValidGitHubUrl(String url) {
        return url != null && url.matches("https://github\\.com/[^/]+/[^/]+\\.git");
    }

    private String getCorrectRemoteUrl(Path repoPath, OAuth2AuthenticationToken authToken) {
        try {
            // Solution 1: Obtenez l'URL depuis le frontend (idéal)
            // Solution 2: Utilisez le nom d'utilisateur GitHub
            Map<String, Object> attributes = ((OAuth2User) authToken.getPrincipal()).getAttributes();
            String username = (String) attributes.get("login");
            String repoName = repoPath.getFileName().toString();

            return "https://github.com/" + username + "/" + repoName + ".git";
        } catch (Exception e) {
            logger.warn("Impossible d'obtenir l'URL correcte du dépôt", e);
            return null;
        }
}}