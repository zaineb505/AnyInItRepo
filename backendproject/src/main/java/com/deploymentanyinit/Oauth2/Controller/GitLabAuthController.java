package com.deploymentanyinit.Oauth2.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/gitlab")
public class GitLabAuthController {

    private final OAuth2AuthorizedClientService clientService;

    public GitLabAuthController(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

 /*   @GetMapping("/token")
    public ResponseEntity<?> checkGitLabToken(OAuth2AuthenticationToken authToken) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                "gitlab", authToken.getName());

        if (client == null || client.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("GitLab token not found");
        }

        return ResponseEntity.ok(Map.of("accessToken", client.getAccessToken().getTokenValue()));
    }*/


    @GetMapping("/token")
    public ResponseEntity<?> getGitLabToken(OAuth2AuthenticationToken authToken) {
        if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                "gitlab",
                authToken.getName());

        if (client == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of(
                "accessToken", client.getAccessToken().getTokenValue(),
                "expiresAt", client.getAccessToken().getExpiresAt()
        ));
    }

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkGitLabAuth(OAuth2AuthenticationToken authToken) {
        if (authToken != null && "gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                    "gitlab", authToken.getName());

            if (client != null && client.getAccessToken() != null) {
                return ResponseEntity.ok(Map.of("authenticated", true));
            }
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
}
