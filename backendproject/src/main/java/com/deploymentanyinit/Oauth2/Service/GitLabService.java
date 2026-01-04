package com.deploymentanyinit.Oauth2.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitLabService {
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate; // À injecter

    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);

    @Value("${git.temp.dir:/tmp/repos}")
    private String tempDir;

    // Templates d'images Docker par langage
    private final Map<String, List<DockerImage>> dockerTemplates = Map.of(
            "nodejs", List.of(
                    new DockerImage("node:20", "Node 20 (latest)"),
                    new DockerImage("node:18", "Node 18 (LTS)"),
                    new DockerImage("node:16", "Node 16")
            ),
         /*   "java", List.of(
                    new DockerImage("openjdk:21", "OpenJDK 21 (latest)"),
                    new DockerImage("openjdk:17", "OpenJDK 17 (LTS)"),
                    new DockerImage("maven:3.8-openjdk-17", "Maven 3.8")
            ),*/

            "java-maven", List.of(
                    new DockerImage("maven:3.8-openjdk-17", "Maven + JDK 17 (LTS)"),
                    new DockerImage("maven:3.9-openjdk-21", "Maven + JDK 21 (Latest)")
            ),
            "java-gradle", List.of(
                    new DockerImage("gradle:8-jdk17", "Gradle + JDK 17"),
                    new DockerImage("gradle:8-jdk21", "Gradle + JDK 21")
            ),

            "python", List.of(
                    new DockerImage("python:3.10", "Python 3.10"),
                    new DockerImage("python:3.9", "Python 3.9")
            )
    );

    public GitLabService(OAuth2AuthorizedClientService authorizedClientService,
                         RestTemplate restTemplate) {
        this.authorizedClientService = authorizedClientService;
        this.restTemplate = restTemplate;

    }

    // === Méthodes publiques ===

    /**
     * Analyse un dépôt et détecte les langages utilisés
     */
    public Set<String> detectLanguages(String repoUrl, OAuth2AuthenticationToken authToken)
            throws IOException, GitAPIException {
        Path repoPath = cloneTempRepo(repoUrl, authToken);
        Set<String> languages = new HashSet<>();

        if (Files.exists(repoPath.resolve("package.json"))) {
            languages.add("nodejs");
        }
        if (Files.exists(repoPath.resolve("pom.xml"))) {
            languages.add("java");
        }
        if (Files.exists(repoPath.resolve("requirements.txt"))) {
            languages.add("python");
        }

        return languages;
    }

    /**
     * Génère le contenu du fichier .gitlab-ci.yml
     */
   /* public String generatePipelineConfig(String language, String dockerImage,
                                         boolean includeBuild, boolean includeTest) {
        StringBuilder yaml = new StringBuilder();

        // Stages
        yaml.append("stages:\n");
        if (includeBuild) yaml.append("  - build\n");
        if (includeTest) yaml.append("  - test\n");
        yaml.append("\n");

        // Job Build
        if (includeBuild) {
            yaml.append("build_job:\n")
                    .append("  stage: build\n")
                    .append("  image: ").append(dockerImage).append("\n")
                    .append("  script:\n");

            switch (language) {
                case "nodejs":
                    yaml.append("    - npm install\n    - npm run build\n");
                    yaml.append("  artifacts:\n    paths:\n      - dist/\n");
                    break;
                case "java":
                    yaml.append("    - mvn package\n");
                    yaml.append("  artifacts:\n    paths:\n      - target/*.jar\n");
                    break;
                case "python":
                    yaml.append("    - pip install -r requirements.txt\n");
                    break;
            }
            yaml.append("\n");
        }

        // Job Test
        if (includeTest) {
            yaml.append("test_job:\n")
                    .append("  stage: test\n")
                    .append("  image: ").append(dockerImage).append("\n")
                    .append("  script:\n");

            switch (language) {
                case "nodejs": yaml.append("    - npm test\n"); break;
                case "java": yaml.append("    - mvn test\n"); break;
                case "python": yaml.append("    - pytest\n"); break;
            }
            yaml.append("\n");
        }

        return yaml.toString();
    }*/

    public String generatePipelineConfig(String language, String dockerImage,
                                         boolean includeBuild, boolean includeTest) {
        StringBuilder yaml = new StringBuilder();

        // Stages
        yaml.append("stages:\n");
        if (includeBuild) yaml.append("  - build\n");
        if (includeTest) yaml.append("  - test\n");
        yaml.append("  - deploy\n\n");

        // Job Build
        if (includeBuild) {
            yaml.append("build_job:\n")
                    .append("  stage: build\n")
                    .append("  image: ").append(dockerImage).append("\n")
                    .append("  script:\n");

            switch (language) {
                case "nodejs" -> {
                    yaml.append("    - npm install\n")
                            .append("    - npm run build\n")
                            .append("  artifacts:\n")
                            .append("    paths:\n")
                            .append("      - dist/\n");
                }
                case "java" -> {
                    yaml.append("    - mvn clean install -DskipTests=true\n")
                            .append("    - mvn package -DskipTests=true\n")
                            .append("  artifacts:\n")
                            .append("    paths:\n")
                            .append("      - target/*.jar\n")
                            .append("    expire_in: 1 hour\n");
                }
                case "python" -> yaml.append("    - pip install -r requirements.txt\n");
            }
            yaml.append("\n");
        }

        // Job Test
        if (includeTest) {
            yaml.append("test_job:\n")
                    .append("  stage: test\n")
                    .append("  image: ").append(dockerImage).append("\n")
                    .append("  script:\n");

            switch (language) {
                case "nodejs" -> yaml.append("    - echo \"Tests ignorés\"\n");
                case "java" -> yaml.append("    - mvn test -DskipTests=true\n");
                case "python" -> yaml.append("    - pytest || true\n");
            }
            yaml.append("\n");
        }

        // Job Deploy (placeholder)
        yaml.append("deploy_job:\n")
                .append("  stage: deploy\n")
                .append("  script:\n")
                .append("    - echo \"Déploiement placeholder...\"\n\n");

        return yaml.toString();
    }


    private String getTestImage(String language, String defaultImage) {
        if ("nodejs".equals(language)) {
            return "cypress/browsers:node20-chrome";
        }
        return defaultImage;
    }

    /**
     * Déploie le pipeline dans le dépôt GitLab
     */


    public void deployPipeline(String repoUrl, String branch, String yamlContent,
                               OAuth2AuthenticationToken authToken) throws IOException, GitAPIException {
        // Vérification explicite que c'est bien un token GitLab
        if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            throw new IllegalStateException("Authentification GitLab requise");
        }
        try {
            Path repoPath = cloneTempRepo(repoUrl, authToken);
            Files.write(repoPath.resolve(".gitlab-ci.yml"), yamlContent.getBytes());
            commitAndPush(repoPath, "[CI/CD Platform] Add automated pipeline", branch, authToken);
        } catch (TransportException e) {
            throw new IOException("Échec du push vers le dépôt. Veuillez vérifier que vous avez les droits d'écriture.", e);
        }
    }

    // === Méthodes utilitaires ===

    private Path cloneTempRepo(String repoUrl, OAuth2AuthenticationToken authToken) throws GitAPIException {
        Path localPath = Paths.get(tempDir, UUID.randomUUID().toString());
        String token = getGitLabToken(authToken);

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath.toFile())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
                .call();

        return localPath;
    }

    private void commitAndPush(Path repoPath, String message, String branch,
                               OAuth2AuthenticationToken authToken) throws IOException, GitAPIException {
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern(".gitlab-ci.yml").call();
            git.commit().setMessage(message).call();
            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", getGitLabToken(authToken)))
                    .call();
        }
    }

   /* private String getGitLabToken(OAuth2AuthenticationToken authToken) {
        // Vérification du fournisseur
        if (!"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            throw new IllegalStateException("Authentification GitLab requise. Fournisseur actuel: " +
                    authToken.getAuthorizedClientRegistrationId());
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName()
        );
        String token = client.getAccessToken().getTokenValue();
        logger.debug("Token GitLab utilisé ({}...)", token.substring(0, 4));
        return token;
    }*/

    /*private String getGitLabToken(OAuth2AuthenticationToken authToken) {
        if (authToken == null || !"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            throw new IllegalStateException("Authentification GitLab requise. Veuillez vous connecter avec GitLab.");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "gitlab",
                authToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Token GitLab non trouvé. Veuillez vous réauthentifier avec GitLab.");
        }

        return client.getAccessToken().getTokenValue();
    }*/


    private String getGitLabToken(OAuth2AuthenticationToken authToken) {
        // Log de débogage pour tracer l'appel
        logger.debug("Tentative de récupération du token GitLab...");

        // Vérification de l'authentification GitLab
        if (authToken == null) {
            logger.error("Aucun token d'authentification fourni");
            throw new IllegalStateException("Authentification requise");
        }

        if (!"gitlab".equals(authToken.getAuthorizedClientRegistrationId())) {
            logger.warn("Mauvais fournisseur OAuth2: {}", authToken.getAuthorizedClientRegistrationId());
            throw new IllegalStateException("Authentification GitLab requise. Actuellement authentifié avec: " +
                    authToken.getAuthorizedClientRegistrationId());
        }

        // Récupération du client OAuth2
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "gitlab",
                authToken.getName()
        );

        // Vérification du token
        if (client == null) {
            logger.error("Aucun client GitLab trouvé pour l'utilisateur");
            throw new IllegalStateException("Session GitLab introuvable");
        }

        if (client.getAccessToken() == null) {
            logger.error("Token d'accès GitLab null pour l'utilisateur");
            throw new IllegalStateException("Token d'accès invalide");
        }

        // Log sécurisé du token (masqué)
        String token = client.getAccessToken().getTokenValue();
        String maskedToken = token.substring(0, 4) + "****" + token.substring(token.length() - 4);
        logger.debug("Token GitLab récupéré (masqué): {}", maskedToken);
        logger.debug("Token expire le: {}", client.getAccessToken().getExpiresAt());

        return token;
    }

/*********************************************************************/

    // Cache mémoire thread-safe
    private final Set<Long> deployedUsers = ConcurrentHashMap.newKeySet();

    public boolean isFirstDeployment(Long gitlabUserId) {
        return !deployedUsers.contains(gitlabUserId);
    }

    public void markAsDeployed(Long gitlabUserId) {
        deployedUsers.add(gitlabUserId);
        logger.info("User {} marked as deployed", gitlabUserId);

    }

    public Long getGitLabUserId(OAuth2AuthenticationToken authToken) {
        String token = getGitLabToken(authToken);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://gitlab.com/api/v4/user",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        return ((Number) response.getBody().get("id")).longValue();
    }


/**********************************************************************/

    // === Getters ===

    public List<DockerImage> getDockerImagesForLanguage(String language) {
        return dockerTemplates.getOrDefault(language, Collections.emptyList());
    }

    // === Classes internes ===

    public static class DockerImage {
        private final String image;
        private final String name;

        public DockerImage(String image, String name) {
            this.image = image;
            this.name = name;
        }

        public String getImage() { return image; }
        public String getName() { return name; }
    }




}