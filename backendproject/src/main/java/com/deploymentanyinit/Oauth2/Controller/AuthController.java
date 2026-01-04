package com.deploymentanyinit.Oauth2.Controller;
import com.deploymentanyinit.Oauth2.Entities.AuthProvider;
import com.deploymentanyinit.Oauth2.Entities.Users;
import com.deploymentanyinit.Oauth2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @GetMapping("/user")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUser(OAuth2AuthenticationToken authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2User oauth2User = authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();
        String provider = authentication.getAuthorizedClientRegistrationId();

        Map<String, Object> response = new HashMap<>();
        response.put("id", attributes.get("id"));
        response.put("name", attributes.get("name") != null ? attributes.get("name") : attributes.get("login"));
        response.put("email", attributes.get("email"));
        response.put("avatarUrl", getAvatarUrl(provider, attributes));
        response.put("provider", provider);
        response.put("authenticated", true);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private String getAvatarUrl(String provider, Map<String, Object> attributes) {
        return "google".equalsIgnoreCase(provider)
                ? (String) attributes.get("picture")
                : (String) attributes.get("avatar_url");
    }

        private String getNameFromAttributes(Map<String, Object> attributes) {
            return Optional.ofNullable((String) attributes.get("name"))
                    .orElse((String) attributes.get("login"));
        }

        private String getEmailFromAttributes(Map<String, Object> attributes, String provider) {
            String email = (String) attributes.get("email");
            if (email == null && "github".equalsIgnoreCase(provider)) {
                return attributes.get("login") + "@users.noreply.github.com";
            }
            return email;
        }

        private String getAvatarUrl(Map<String, Object> attributes, String provider) {
            if ("google".equalsIgnoreCase(provider)) {
                return (String) attributes.get("picture");
            }
            return (String) attributes.get("avatar_url");
        }

        private String getUsername(Map<String, Object> attributes, String provider) {
            if ("google".equalsIgnoreCase(provider)) {
                return (String) attributes.get("email");
            }
            return (String) attributes.get("login");
        }

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAccessToken(OAuth2AuthenticationToken authentication) {
        String registrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId, principalName);

        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            Map<String, String> response = new HashMap<>();
            response.put("access_token", accessToken);

            // Ajout des headers CORS
            return ResponseEntity.ok()
                  //.header("Access-Control-Allow-Origin", "http://localhost:4200")
                  // .header("Access-Control-Allow-Credentials", "true")
                    .body(response);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token GitHub introuvable pour cet utilisateur"
            );
        }
    }

}