package com.deploymentanyinit.Oauth2.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.server.LogStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


@Slf4j
@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    private final Path tempDir;
    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
    private SimpMessagingTemplate messagingTemplate;
    private LogWebSocketPublisher logWebSocketPublisher;


    @Autowired
    public DockerService(DockerClient dockerClient,
                         OAuth2AuthorizedClientService oAuth2ClientService,
                         LogWebSocketPublisher logWebSocketPublisher,

                         @Value("${app.temp.dir:/tmp/docker-builds}") String tempDirPath) throws IOException {
        this.dockerClient = dockerClient;
        this.authorizedClientService = oAuth2ClientService;
        this.tempDir = Paths.get(tempDirPath);
        Files.createDirectories(this.tempDir);
        this.logWebSocketPublisher = logWebSocketPublisher;

    }

    public DeploymentResult deployFromGit(String repoUrl,
                                          String imageName,
                                          int hostPort,
                                          int containerPort,
                                          OAuth2AuthenticationToken authToken) throws Exception {
        logger.info("Début du déploiement Docker pour le dépôt: {}", repoUrl);
        logWebSocketPublisher.sendLog("Début du déploiement Docker pour le dépôt: " + repoUrl);

        Path repoPath = cloneTempRepo(repoUrl, authToken);
        try {
            String projectType = detectProjectType(repoPath);
            String projectName = detectProjectName(repoPath); // Nouvelle méthode

            String buildLogs = buildWithPack(repoPath, imageName, projectType, projectName);
            logger.info("Type de projet: {}, Nom: {}", projectType, projectName);
            logWebSocketPublisher.sendLog(" Projet détecté : " + projectType + " - " + projectName);

            logger.info("Construction de l'image Docker réussie: {}", imageName);
            logWebSocketPublisher.sendLog(" Construction de l'image Docker réussie: " + imageName);

            String containerId = runContainer(imageName, hostPort, containerPort, projectType);
            logger.info("Conteneur démarré avec ID: {}", containerId);
            logWebSocketPublisher.sendLog(" Conteneur démarré avec ID: " + containerId);

            return new DeploymentResult(containerId, buildLogs);
        } finally {
            cleanupTempFiles(repoPath);
            FileUtils.deleteDirectory(repoPath.toFile());
        }
    }

    private void cleanupTempFiles(Path path) {
        try {
            Files.deleteIfExists(path.resolve("nginx.conf"));
        } catch (IOException e) {
            logger.warn("Échec suppression nginx.conf temporaire", e);
        }
    }
    // Détection du nom du projet depuis package.json
    private String detectProjectName(Path projectPath) throws IOException {
        Path packageJsonPath = projectPath.resolve("package.json");

        if (!Files.exists(packageJsonPath)) {
            throw new IOException("package.json introuvable");
        }

        String content = Files.readString(packageJsonPath);
        JsonObject packageJson = JsonParser.parseString(content).getAsJsonObject();

        if (!packageJson.has("name")) {
            throw new IOException("La propriété 'name' est manquante dans package.json");
        }

        String projectName = packageJson.get("name").getAsString();
        return cleanProjectName(projectName);
    }

    // Nettoyage du nom de projet
    private String cleanProjectName(String rawName) {
        // Supprime le scope npm (@organisation/) si présent
        return rawName.replaceAll("^@[^/]+/", "");
    }

    private String detectProjectType(Path projectPath) throws IOException {
        if (Files.exists(projectPath.resolve("pom.xml"))) {
            return "JAVA";
        } else if (Files.exists(projectPath.resolve("angular.json"))) {
            String content = Files.readString(projectPath.resolve("angular.json"));
            Pattern pattern = Pattern.compile("\"defaultProject\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                System.setProperty("ANGULAR_PROJECT", matcher.group(1));
            }
            return "ANGULAR";

        } else if (Files.exists(projectPath.resolve("requirements.txt"))) {
            return "PYTHON";
        }
        return "UNKNOWN";
    }

    private void deleteRepoDirectory(Path repoPath) {
        try {
            Files.walkFileTree(repoPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // Supprime d'abord les fichiers
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir); // Puis les répertoires
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Erreur suppression répertoire: {}", e.getMessage());
        }
    }

    private Path cloneTempRepo(String repoUrl, OAuth2AuthenticationToken authToken) throws GitAPIException, IOException {
        String token = getGitLabToken(authToken);
        Path localPath = tempDir.resolve(UUID.randomUUID().toString());
        logger.debug("Clonage du dépôt: {} vers: {}", repoUrl, localPath);

        Git git = null;
        try {
            // Cloner le dépôt
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localPath.toFile())
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", token))
                    .call();
            logger.info("Vérification du dossier cloné: {} - Existe? {}", localPath, Files.exists(localPath));

            // Fermeture explicite des ressources Git
            if (git != null) {
                git.getRepository().close();
                git.close();
            }

            // Appel au garbage collector pour libérer les ressources (utile sous Windows)
            System.gc();

            // Suppression du dossier .git avec mécanisme de retry amélioré
            supprimerDossierGitAvecRetry(localPath);

            return localPath;
        } finally {
            if (git != null) {
                try {
                    git.getRepository().close();
                    git.close();
                } catch (Exception e) {
                    logger.warn("Erreur lors de la fermeture des ressources Git", e);
                }
            }
        }
    }

    private void supprimerDossierGitAvecRetry(Path repoPath) {
        Path gitFolder = repoPath.resolve(".git");
        if (!Files.exists(gitFolder)) {
            return;
        }

        int maxTentatives = 5;  // Nombre de tentatives augmenté
        int delaiEntreTentativesMs = 1000;

        for (int tentative = 1; tentative <= maxTentatives; tentative++) {
            try {
                // D'abord rendre les fichiers modifiables (important pour Windows)
                rendreFichiersModifiables(gitFolder);

                // Puis suppression
                Files.walk(gitFolder)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                logger.info("Dossier .git supprimé avec succès");
                return;
            } catch (IOException | UncheckedIOException e) {
                logger.warn("Échec de suppression du dossier .git (tentative {}/{}): {}", tentative, maxTentatives, e.getMessage());
                if (tentative < maxTentatives) {
                    try {
                        Thread.sleep(delaiEntreTentativesMs);
                        // Appel supplémentaire au GC entre les tentatives
                        System.gc();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        logger.error("Échec définitif de suppression du dossier .git: {}", gitFolder);
    }

    private void rendreFichiersModifiables(Path path) throws IOException {
        Files.walk(path)
                .forEach(p -> {
                    try {
                        File file = p.toFile();
                        if (file.exists()) {
                            // Supprimer l'attribut lecture seule
                            file.setWritable(true);
                            // Pour Windows, traitement spécifique
                            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                Files.setAttribute(p, "dos:readonly", false);
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }


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


    /***********************   THE ONE  BUILDWITHPACK() THAT I USE ALWAYS ********************************/

   /* private String buildWithPack(Path sourcePath, String imageName, String projectType, String projectName) throws IOException, InterruptedException {
        String vmUser = "useradm";
        String vmIp = "192.168.1.22";
        String vmPath = "/tmp/docker-builds/test-build2/" + UUID.randomUUID();

        // 1. Normalisation du chemin source (Windows -> Linux)
        String normalizedSource = sourcePath.toString().replace("\\", "/");

        // 2. Création du dossier avec vérification
        String mkdirCommand = String.format(
                "mkdir -p %s && chmod -R 777 %s",
                vmPath, vmPath
        );

        ProcessBuilder mkdirPb = new ProcessBuilder(
                "ssh",
                vmUser + "@" + vmIp,
                mkdirCommand
        );

        // Capture des erreurs SSH
        Process mkdirProcess = mkdirPb.start();
        String sshError = new String(mkdirProcess.getErrorStream().readAllBytes());
        int exitCode = mkdirProcess.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Échec création dossier: " + vmPath +
                            "\nErreur SSH: " + sshError
            );
        }
        // AJOUTEZ avant la copie :
        ProcessBuilder cleanPb = new ProcessBuilder(
                "ssh",
                vmUser + "@" + vmIp,
                "rm -rf " + vmPath + "/dist && mkdir -p " + vmPath + "/dist"

        );


        Process cleanProcess = cleanPb.start();
        if (cleanProcess.waitFor() != 0) {
            throw new RuntimeException("Failed to clean dist directory");
        }
        // 3. Copie unique avec vérification
        ProcessBuilder copyPb = new ProcessBuilder(
                "scp", "-r",
                normalizedSource + "/*",  // Copie le contenu du dossier
                //normalizedSource,

                vmUser + "@" + vmIp + ":" + vmPath
        );

        Process copyProcess = copyPb.start();
        exitCode = copyProcess.waitFor();

        if (exitCode != 0) {
            String scpError = new String(copyProcess.getErrorStream().readAllBytes());
            throw new RuntimeException(
                    "Échec copie vers VM: " + scpError
            );
        }

        // Exécution à distance via ssh
        ProcessBuilder packPb = new ProcessBuilder(
                "ssh",
                vmUser + "@" + vmIp,
                "pack",
                "build", imageName,
                "--path", vmPath,
                // "--builder", "paketobuildpacks/builder:base",
                "--builder", "paketobuildpacks/builder-jammy-base",
                "--pull-policy", "if-not-present",
                //"--default-process", "web",  // <-- Ajoutez cette ligne
                "--trust-builder",
                "--network", "host",
                // "--run-image", "paketobuildpacks/run-jammy-base",
                "-v",  // Mode verbeux
                "--timestamps", // Ajoute des timestamps
                "--env", "BP_LOG_LEVEL=DEBUG", // Logs des buildpacks
                "--env", "PACK_LOG_LEVEL=debug" // Logs internes de pack
        );

        // Configuration spécifique au type de projet
        configureForProjectType(packPb, projectType, projectName, sourcePath);
        StringBuilder logsBuilder = new StringBuilder();
        Process process = packPb.start();

        // Thread pour stdout
        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String logLine = "[PACK] " + line;
                    logsBuilder.append(logLine).append("\n");
                    logger.info(logLine);
                    if (isImportantForUser(line)) {
                        logWebSocketPublisher.sendLog(logLine);
                    }
                }
            } catch (IOException e) {
                logger.error("Erreur lecture stdout", e);
            }
        });

        // Thread pour stderr
        Thread errThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String errLine = "[PACK-ERR] " + line;
                    logsBuilder.append(errLine).append("\n");
                    logger.error(errLine);
                    if (isImportantForUser(line)) {
                        logWebSocketPublisher.sendLog(errLine);
                    }
                }
            } catch (IOException e) {
                logger.error("Erreur lecture stderr", e);
            }
        });

        outThread.start();
        errThread.start();

        exitCode = process.waitFor();
        outThread.join();
        errThread.join();
        if (exitCode != 0) {
            throw new RuntimeException("Build failed (code " + exitCode + "):\n" + outThread);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException("Thread interrompu pendant pack build");
        }


        return outThread.toString();

    }*/



    private String buildWithPack(Path sourcePath, String imageName, String projectType, String projectName) throws IOException, InterruptedException {

        ProcessBuilder packPb = new ProcessBuilder(
                "pack", "build", imageName,
                "--path", sourcePath.toString(),
                "--builder", "paketobuildpacks/builder-jammy-base",
                "--pull-policy", "if-not-present",
                "--trust-builder",
                "--network", "host",
                "-v", "--timestamps",
                "--env", "BP_LOG_LEVEL=DEBUG",
                "--env", "PACK_LOG_LEVEL=debug"
        );
        packPb.environment().put("PATH", System.getenv("PATH") + ":/usr/local/bin");
        // Ajouter les paramètres spécifiques selon le type de projet
        configureForProjectType(packPb, projectType, projectName, sourcePath);

        StringBuilder logsBuilder = new StringBuilder();
        Process process = packPb.start();

        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logsBuilder.append("[PACK] ").append(line).append("\n");
                    logger.info(line);
                    logWebSocketPublisher.sendLog(line);
                }
            } catch (IOException e) { logger.error("Erreur stdout", e); }
        });

        Thread errThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logsBuilder.append("[PACK-ERR] ").append(line).append("\n");
                    logger.error(line);
                    logWebSocketPublisher.sendLog(line);
                }
            } catch (IOException e) { logger.error("Erreur stderr", e); }
        });

        outThread.start();
        errThread.start();
        int exitCode = process.waitFor();
        outThread.join();
        errThread.join();

        if (exitCode != 0) {
            throw new RuntimeException("Build failed (code " + exitCode + ")");
        }

        return logsBuilder.toString();
    }


    private boolean isImportantForUser(String line) {
        return line.contains("Building")
                || line.contains("Completed")
                || line.contains("Error")
                || line.contains("Installing")
                || line.contains("Downloading")
                || line.contains("Running")
                || line.contains("Successfully")
                || line.contains("Saving")
                || line.contains("*** Images")
                || line.contains("Image ID")

                || line.contains("Executing");
    }

    public Optional<String> getProjectName(Path projectPath) {
        try {
            String content = Files.readString(projectPath.resolve("package.json"));
            return Optional.ofNullable(content)
                    .map(JsonParser::parseString)
                    .map(json -> json.getAsJsonObject().get("name"))
                    .map(name -> name.getAsString().replaceAll("^@[^/]+/", ""));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private void configureForProjectType(ProcessBuilder packPb, String projectType, String projectName,Path sourcePath) {
        switch (projectType) {
            case "JAVA":
               /* packPb.command().add("--env");
                packPb.command().add("BP_JVM_TYPE=JRE");
                //packPb.command().add("--env");
                //packPb.command().add("BP_MAVEN_BUILD_ARGUMENTS=-Dmaven.test.skip=true clean package");
                packPb.command().add("--env");
                packPb.command().add("BP_MAVEN_BUILT_ARTIFACT=target/*.jar");*/
                packPb.command().add("--env");
                packPb.command().add("BP_JVM_VERSION=17"); // Spécifiez la version Java

                packPb.command().add("--env");
                packPb.command().add("BP_MAVEN_BUILT_ARTIFACT=target/*.jar");
                break;
            case "ANGULAR":
                // Ajoutez ces variables d'environnement critiques
                packPb.command().add("--env");
                packPb.command().add("BP_NODE_VERSION=18"); // Version LTS stable
                packPb.command().add("--env");
                packPb.command().add("BP_NPM_CI_FLAGS=--legacy-peer-deps");
                packPb.command().add("--env");
                packPb.command().add("NODE_OPTIONS=--openssl-legacy-provider");

                packPb.command().add("--env");
                packPb.command().add("BP_NODE_RUN_SCRIPTS=build");
                packPb.command().add("--env");
                packPb.command().add("BP_WEB_SERVER=nginx");
                packPb.command().add("--env");
                //packPb.command().add("BP_WEB_SERVER_ROOT=dist");
                packPb.command().add("BP_WEB_SERVER_ROOT=dist/" + projectName + "/browser");
                // Nouvelle configuration NGINX améliorée
                String nginxConf = """
                    server {
                        listen ${PORT:-8080};
                        root /workspace/;
                        index index.html;
                        
                        location / {
                            try_files $uri $uri/ /index.html;
                        }
                        
                        # Gestion des assets
                        location ~* \\.(eot|ttf|woff|woff2|svg|png|jpg|jpeg|gif|ico)$ {
                            expires 1y;
                            access_log off;
                            add_header Cache-Control "public";
                        }
                        
                        # Désactivation des logs
                        access_log off;
                        error_log /dev/null;
                    }""";


                try {
                    Files.writeString(
                            sourcePath.resolve("nginx.conf"),
                            nginxConf,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                } catch (IOException e) {
                    logger.error("Échec écriture nginx.conf", e);
                    throw new RuntimeException(e);
                }

                packPb.command().add("--env");
                packPb.command().add("BP_NODE_INSTALL_ANGULAR_CLI=true");
                packPb.command().add("--env");
                packPb.command().add("BP_NODE_PROJECT_PATH=./");
                packPb.command().add("--env");
                packPb.command().add("NODE_ENV=production");
                packPb.command().add("--env");
                packPb.command().add("DISABLE_PRERENDERING=true");
                packPb.command().add("--env");
                packPb.command().add("NG_BUILD_DISABLE_FONTS_INLINING=true"); // Désactive l'inlining
                packPb.command().add("--env");
                packPb.command().add("NG_BUILD_OFFLINE=true"); // Mode hors-ligne

                // ... autres cas
        }
    }

    private String detectAngularAppName(Path projectPath) throws IOException {
        Path angularJson = projectPath.resolve("angular.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(angularJson.toFile());
        return root.path("projects").fieldNames().next(); // Prend le premier projet
    }

    public String runContainer(String imageName, int hostPort, int containerPort, String projectType) {
        try {
            checkPortAvailability(hostPort);

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withExposedPorts(ExposedPort.tcp(containerPort))
                    .withPortBindings(new Ports(ExposedPort.tcp(containerPort), Ports.Binding.bindPort(hostPort)))
                    // .withCmd(getStartCommand(projectType, containerPort))
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();
            //logContainerLogs(container.getId());
            return container.getId();
        } catch (Exception e) {
            throw new RuntimeException("Échec du lancement: " + e.getMessage(), e);
        }
    }

    private String[] getStartCommand(String projectType, int port) {
        return switch (projectType) {
            case "JAVA" -> new String[]{"java", "-jar", "/target/*.jar"};

            case "ANGULAR" -> new String[]{
                    "sh",
                    "-c",
                    "npm run build && nginx -g 'daemon off;' -c /etc/nginx/nginx.conf"
            };
            case "NODEJS" -> new String[]{"npm", "start"};
            case "PYTHON" -> new String[]{"gunicorn", "app:app", "--bind", "0.0.0.0:" + port};
            default -> new String[]{"/cnb/process/web"};
        };
    }
    private Path findFirstJar(String directory) {
        try (Stream<Path> stream = Files.list(Paths.get(directory))) {
            return stream.filter(p -> p.toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucun JAR trouvé dans " + directory));
        } catch (IOException e) {
            throw new RuntimeException("Erreur recherche JAR: " + e.getMessage());
        }
    }
    private HealthCheck createHealthCheck(int port) {
        return new HealthCheck()
                .withTest(List.of("CMD-SHELL", "curl -f http://localhost:" + port + " || exit 1"))
                .withInterval(1000000000L)
                .withTimeout(500000000L)
                .withRetries(3);
    }



    public void checkPortAvailability(int port) throws IOException {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.close();
        } catch (IOException e) {
            throw new IOException("Port " + port + " est déjà utilisé");
        }
    }

    /************************************************************/
    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }

    public static class DeploymentResult {
        private final String containerId;
        private final String buildLogs;

        public DeploymentResult(String containerId, String buildLogs) {
            this.containerId = containerId;
            this.buildLogs = buildLogs;
        }

        // Getters
        public String getContainerId() { return containerId; }
        public String getBuildLogs() { return buildLogs; }
    }
    public List<ContainerInfo> listContainersInfo() {
        return dockerClient.listContainersCmd().exec().stream()
                .map(container -> new ContainerInfo(
                        container.getId(),
                        container.getImage(),
                        container.getStatus()
                ))
                .toList();
    }

    //DTO
    public class ContainerInfo {
        private String id;
        private String image;
        private String status;

        public ContainerInfo(String id, String image, String status) {
            this.id = id;
            this.image = image;
            this.status = status;
        }

        public String getId() { return id; }
        public String getImage() { return image; }
        public String getStatus() { return status; }
    }
}