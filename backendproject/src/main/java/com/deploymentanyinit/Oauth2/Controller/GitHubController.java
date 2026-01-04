package com.deploymentanyinit.Oauth2.Controller;
import com.deploymentanyinit.Oauth2.Service.GitHubService;
import com.deploymentanyinit.Oauth2.Service.JGitService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {
    @GetMapping
    public ResponseEntity<String> getTest() {
        return ResponseEntity.ok("API GitHub fonctionnelle");
    }

    private final GitHubService gitHubService;
    private final OAuth2AuthorizedClientService authorizedClientService;


    @Autowired
    public GitHubController(GitHubService gitHubService,
                            OAuth2AuthorizedClientService authorizedClientService) {
        this.gitHubService = gitHubService;
        this.authorizedClientService = authorizedClientService;
    }


@GetMapping("/repos")
public ResponseEntity<List<GitHubRepo>> getUserRepos(HttpServletRequest request) {
    // Vérifier d'abord l'authentification courante
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    OAuth2AuthenticationToken githubAuth = null;

    if (authentication instanceof OAuth2AuthenticationToken) {
        OAuth2AuthenticationToken currentAuth = (OAuth2AuthenticationToken) authentication;
        if ("github".equals(currentAuth.getAuthorizedClientRegistrationId())) {
            githubAuth = currentAuth;
        }
    }

    // Si l'authentification courante n'est pas GitHub, vérifier la session
    if (githubAuth == null) {
        githubAuth = (OAuth2AuthenticationToken) request.getSession().getAttribute("OAUTH2_GITHUB");
    }

    if (githubAuth == null) {
        return ResponseEntity.status(401).build();
    }

    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            githubAuth.getAuthorizedClientRegistrationId(),
            githubAuth.getName());

    if (client == null) {
        return ResponseEntity.status(401).build();
    }

    String accessToken = client.getAccessToken().getTokenValue();
    List<GitHubRepo> repos = gitHubService.getUserRepositories(accessToken);

    return ResponseEntity.ok(repos);
}
@Value("${git.clone.directory:/tmp/git-clones}") // Chemin configurable
private String cloneDirectory;

    @Autowired
    private JGitService jGitService;
    private String extractRepoName(String repoUrl) {
        return repoUrl.replaceAll(".*[/:]([^/]+?)(\\.git)?$", "$1");
    }
////////////////clone////////////////////////
   /* @PostMapping("/clone")
    public ResponseEntity<Map<String, String>> cloneRepository(
            @RequestParam String repoUrl,
            @RequestParam String branch,
            OAuth2AuthenticationToken authToken) {

        try {
            Path targetPath = Paths.get(cloneDirectory).resolve(extractRepoName(repoUrl));
            jGitService.cloneRepository(repoUrl, targetPath, branch, authToken);

            return ResponseEntity.ok(Map.of(
                    "message", "Clone réussi",
                    "path", targetPath.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }*/
////////////////////commitandpush/////////////////////////
@PostMapping("/commit-and-push")
public ResponseEntity<Map<String, String>> commitAndPush(
        @RequestParam String repoPath,
        @RequestParam String commitMessage,
        @RequestParam(defaultValue = "main") String branchName, // Valeur par défaut

        @RequestParam(required = false) String remoteUrl,
        OAuth2AuthenticationToken authToken) {

    try {
        Path repoDir = Paths.get(repoPath.replace("\\", "/"));
        jGitService.commitAndPush(repoDir, commitMessage, branchName, remoteUrl, authToken);

        return ResponseEntity.ok(Map.of(
                "message", "Push réussi"
        ));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
    }
}
    ///////////////////clonetodesktop/////////////////
    @PostMapping("/clone-to-desktop")
    public ResponseEntity<Map<String, String>> cloneToDesktop(
            @RequestParam String repoUrl,
            @RequestParam String branch,
            OAuth2AuthenticationToken authToken) {

        try {
            Path desktopPath = jGitService.cloneToDesktop(repoUrl, branch, authToken);

            return ResponseEntity.ok(Map.of(
                    "message", "Dépôt cloné sur votre Bureau",
                    "path", desktopPath.toString(),
                    "os", System.getProperty("os.name")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Échec du clonage : " + e.getMessage(),
                    "solution", "Vérifiez les permissions du dossier Bureau"
            ));
        }
    }
    /////////////////////pull//////////////////
    @PostMapping("/pull-existing")
    public ResponseEntity<Map<String, Object>> pullExistingRepository(
            @RequestParam String repoPath,
            @RequestParam String branch,
            @RequestParam(required = false) String remoteUrl, // Nouveau paramètre
            OAuth2AuthenticationToken authToken) {

        try {
            Path repoDir = Paths.get(repoPath.replace("\\", "/"));

            if (!Files.exists(repoDir.resolve(".git"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Le chemin spécifié n'est pas un dépôt Git valide"
                ));
            }

            Map<String, Object> result = jGitService.pullRepository(repoDir, branch, authToken, remoteUrl);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Erreur serveur: " + e.getMessage()
            ));
        }
    }}