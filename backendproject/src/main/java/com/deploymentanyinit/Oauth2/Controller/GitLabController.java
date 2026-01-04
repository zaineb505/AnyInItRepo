package com.deploymentanyinit.Oauth2.Controller;

import com.deploymentanyinit.Oauth2.Service.GitLabService;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/gitlab")
public class GitLabController {
    private final GitLabService gitLabService;
    private final OAuth2AuthorizedClientService clientService;

    public GitLabController(GitLabService gitLabService,
                            OAuth2AuthorizedClientService clientService) {
        this.gitLabService = gitLabService;
        this.clientService = clientService;

    }

    @GetMapping("/detect-languages")
    public ResponseEntity<LanguageDetectionResponse> detectLanguages(
            @RequestParam String repoUrl,
            OAuth2AuthenticationToken authToken) throws IOException, GitAPIException {

        Set<String> languages = gitLabService.detectLanguages(repoUrl, authToken);
        Map<String, List<GitLabService.DockerImage>> availableImages = new HashMap<>();

        languages.forEach(lang ->
                availableImages.put(lang, gitLabService.getDockerImagesForLanguage(lang))
        );

        return ResponseEntity.ok(new LanguageDetectionResponse(languages, availableImages));
    }



    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Boolean>> checkGitLabAuth(OAuth2AuthenticationToken authToken) {
        boolean authenticated = false;
        if (authToken != null && "gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    "gitlab",
                    authToken.getName()
            );
            authenticated = client != null && client.getAccessToken() != null;
        }
        return ResponseEntity.ok(Map.of("authenticated", authenticated));
    }
    @PostMapping(value = "/generate-pipeline", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generatePipeline(
            @RequestBody PipelineGenerationRequest request) {

        String yaml = gitLabService.generatePipelineConfig(
                request.language(),
                request.dockerImage(),
                request.includeBuild(),
                request.includeTest()
        );

        return ResponseEntity.ok(yaml);
    }
/** the one and only work **/
 /*@PostMapping("/deploy-pipeline")
    public ResponseEntity<String> deployPipeline(

         @RequestBody PipelineDeploymentRequest request,
            OAuth2AuthenticationToken authToken) throws IOException, GitAPIException {

        gitLabService.deployPipeline(
                request.repoUrl(),
                request.branch(),
                request.yamlContent(),
                authToken
        );

        return ResponseEntity.ok("Pipeline déployé avec succès !");

     // Return JSON response instead of plain text
    }*/

@PostMapping("/deploy-pipeline")
public ResponseEntity<Map<String, String>> deployPipeline(
        @RequestBody PipelineDeploymentRequest request,
        HttpServletRequest httpRequest,
        OAuth2AuthenticationToken authToken) {

    // Vérifier explicitement que nous sommes avec GitLab
    if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
        // Récupérer depuis la session si disponible
        authToken = (OAuth2AuthenticationToken) httpRequest.getSession()
                .getAttribute("OAUTH2_GITLAB");

        if (authToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentification GitLab requise"));
        }
    }

    try {
        // 1. Vérification automatique


        // 2. Déploiement
        gitLabService.deployPipeline(
                request.repoUrl(),
                request.branch(),
                request.yamlContent(),
                authToken
        );

        return ResponseEntity.ok(Map.of("status", "success"));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "error", e.getMessage()
                ));
    }
}

/** NEW dep pipeline */
    /*@PostMapping("/deploy-pipeline")
    public ResponseEntity<?> deployPipeline(
            @RequestBody PipelineDeploymentRequest request,
            @RequestHeader(value = "X-GitLab-Auth", required = false) String gitlabAuthHeader,
            OAuth2AuthenticationToken authToken) {

        try {
            // Vérification explicite de l'authentification GitLab
            if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", "GitLab authentication required",
                                "authUrl", "/oauth2/authorization/gitlab"
                        ));
            }
            // Ensuite vérifier l'authentification GitLab
            if (gitlabAuthHeader == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", "GitLab authentication required",
                                "authUrl", "/oauth2/authorization/gitlab"
                        ));
            }

            // Créer un token GitLab artificiel pour le service
          //  OAuth2AuthenticationToken gitlabAuthToken = createGitLabToken(gitlabAuthHeader);


            gitLabService.deployPipeline(
                    request.repoUrl(),
                    request.branch(),
                    request.yamlContent(),
                    authToken
            );

            return ResponseEntity.ok("Pipeline déployé avec succès !");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors du déploiement: " + e.getMessage()));
        }
    }*/

    /************************************************************************************/
    @GetMapping("/user-status")
    public ResponseEntity<Map<String, Object>> getUserStatus(
            OAuth2AuthenticationToken authToken) {

        Long userId = gitLabService.getGitLabUserId(authToken);
        return ResponseEntity.ok(Map.of(
                "is_first_deployment", gitLabService.isFirstDeployment(userId),
                "gitlab_user_id", userId
        ));
    }

    @PostMapping("/mark-deployed")
    public ResponseEntity<Void> markAsDeployed(
            @RequestBody Map<String, Long> request, // Changez ici
            OAuth2AuthenticationToken authToken) {

        Long gitlabUserId = request.get("gitlabUserId");
        if (gitlabUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        gitLabService.markAsDeployed(gitlabUserId);
        return ResponseEntity.ok().build();
    }
    /************************************************************************************/






    // === Records pour les DTOs ===

    public record LanguageDetectionResponse(
            Set<String> languages,
            Map<String, List<GitLabService.DockerImage>> availableImages
    ) {}

    public record PipelineGenerationRequest(
            String language,
            String dockerImage,
            boolean includeBuild,
            boolean includeTest
    ) {}

    public record PipelineDeploymentRequest(
            String repoUrl,
            String branch,
            String yamlContent
    ) {}

    /************************/
    public record UserStatusResponse(
            long gitlab_user_id,
            boolean is_first_deployment
    ) {}

    /***********************/
}