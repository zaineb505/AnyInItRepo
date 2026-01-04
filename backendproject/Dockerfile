FROM maven:3.9.5-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : Image d’exécution légère
FROM eclipse-temurin:21-jdk-alpine


ARG DOCKER_GID=997
USER root

# Installer les outils nécessaires
RUN apk add --no-cache curl docker-cli openssh

# Créer le groupe docker avec le bon GID (passé via ARG)
RUN addgroup -g ${DOCKER_GID} docker

# Créer l'utilisateur spring et l'ajouter au groupe docker
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    adduser spring docker

# Installer Pack CLI
RUN curl -sSL "https://github.com/buildpacks/pack/releases/download/v0.37.0/pack-v0.37.0-linux.tgz" | tar -xz && \
    mv pack /usr/local/bin/

# Créer le répertoire de travail
RUN mkdir /app && chown spring:spring /app
USER spring
WORKDIR /app

# Copier le .jar compilé
COPY --from=build /app/target/*Oauth2-0.0.1-SNAPSHOT.jar app.jar

# Définir les variables d’environnement
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]