package com.deploymentanyinit.Oauth2.Controller;
import com.deploymentanyinit.Oauth2.Service.DockerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@RestController
@RequestMapping("/api/docker")
public class DockerController {

    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @PostMapping(value = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deploy(
            @RequestParam String repoUrl,
            @RequestParam(required = false) String imageName,
            @RequestParam int hostPort,  // Port hôte
            @RequestParam(required = false, defaultValue = "8080") int containerPort, // Port conteneur
            HttpServletRequest httpRequest,
            OAuth2AuthenticationToken authToken) {
        if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentification GitLab requise"));
        }

        // Vérification spécifique à GitLab
        if (!"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Authentification GitLab requise"));
        }


        try {
            // Vérification de la disponibilité du port
            dockerService.checkPortAvailability(hostPort);
            // Génération du nom d'image si non fourni
            String finalImageName = (imageName == null || imageName.isEmpty())
                    ? "auto-" + UUID.randomUUID().toString().substring(0, 8)
                    : imageName;

            DockerService.DeploymentResult result = dockerService.deployFromGit(
                    repoUrl,
                    finalImageName,
                    hostPort,
                    containerPort,
                    authToken
            );

            return ResponseEntity.ok(Map.of(
                    "containerId", result.getContainerId(),
                    "port", hostPort,
                    "status", "running",
                    "buildLogs", result.getBuildLogs()
            ));
        } catch (Exception e) {
            log.error("Erreur de déploiement", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Échec du déploiement",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Boolean>> checkAuth(OAuth2AuthenticationToken authToken) {
        boolean authenticated = authToken != null &&
                "gitlab".equals(authToken.getAuthorizedClientRegistrationId());
        return ResponseEntity.ok(Map.of("authenticated", authenticated));
    }
}