package com.deploymentanyinit.Oauth2.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate; // Doit être ceci, pas un autre package
import com.deploymentanyinit.Oauth2.Controller.GitHubRepo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class GitHubService {

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientService clientService; // Ajouté

    // Injection par constructeur


    public GitHubService(RestTemplate restTemplate,
                         OAuth2AuthorizedClientService clientService) {
        this.restTemplate = restTemplate;
        this.clientService = clientService;
    }
/** the and only getuserrepositories **/
  /*  public List<GitHubRepo> getUserRepositories(String accessToken) {
        // 1. Configurez les headers CORRECTEMENT (Spring HttpHeaders)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        // 2. Créez l'entité HTTP
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 3. Exécutez la requête
        try {
            ResponseEntity<GitHubRepo[]> response = restTemplate.exchange(
                    "https://api.github.com/user/repos",
                    HttpMethod.GET,
                    entity,
                    GitHubRepo[].class); // Utilisez votre classe DTO

            // 4. Traitez la réponse
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            // Loggez l'erreur
            e.printStackTrace();
        }
        return Collections.emptyList();
    }*/


public List<GitHubRepo> getUserRepositories(String authToken) {

    // Changez la signature pour accepter le token directement
        return restTemplate.exchange(
                        "https://api.github.com/user/repos",
                        HttpMethod.GET,
                        new HttpEntity<>(createHeaders(authToken)),
                        new ParameterizedTypeReference<List<GitHubRepo>>() {})
                .getBody();
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token); // Notez "token" au lieu de "Bearer"
        return headers;
    }

}

/*public List<GitHubRepo> getUserRepositories(OAuth2AuthenticationToken authToken) {
    OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
            "github",
            authToken.getName()
    );

    if (client == null) {
        throw new IllegalStateException("Client GitHub non authentifié");
    }


    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(client.getAccessToken().getTokenValue());
    headers.set("Accept", "application/vnd.github.v3+json");
    headers.set("User-Agent", "Your-App-Name"); // Nécessaire pour GitHub

    try {
        ResponseEntity<GitHubRepo[]> response = restTemplate.exchange(
                "https://api.github.com/user/repos?per_page=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GitHubRepo[].class
        );
        return Arrays.asList(response.getBody());
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            // Token expiré ou invalide
            throw new SecurityException("Token GitHub invalide", e);
        }
        throw e;
    }
}*/





















/*
@Service
public class GitHubService {

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientService clientService;

    public GitHubService(RestTemplate restTemplate,
                         OAuth2AuthorizedClientService clientService) {
        this.restTemplate = restTemplate;
        this.clientService = clientService;
    }

    public List<GitHubRepo> getUserRepositories(OAuth2AuthenticationToken authToken) {
        if (authToken == null || !"github".equals(authToken.getAuthorizedClientRegistrationId())) {
            throw new IllegalArgumentException("Token GitHub invalide");
        }

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                "github",
                authToken.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new SecurityException("Authentification GitHub requise");
        }

        HttpEntity<?> entity = createGitHubRequestEntity(client.getAccessToken().getTokenValue());

        try {
            ResponseEntity<GitHubRepo[]> response = restTemplate.exchange(
                    "https://api.github.com/user/repos?per_page=100&sort=updated",
                    HttpMethod.GET,
                    entity,
                    GitHubRepo[].class);

            return Arrays.asList(Objects.requireNonNull(response.getBody()));

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new SecurityException("Token GitHub invalide ou expiré", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur GitHub API: " + e.getMessage(), e);
        }
    }

    private HttpEntity<?> createGitHubRequestEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "DeploymentAnyInit-App");
        return new HttpEntity<>(headers);
    }
}*/