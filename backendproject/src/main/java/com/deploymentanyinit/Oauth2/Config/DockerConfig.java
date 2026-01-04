package com.deploymentanyinit.Oauth2.Config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {

    @Bean
    public DockerClient dockerClient() {
        // Configuration pour socket Unix
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                //.withDockerHost("tcp://192.168.1.22:2375")
                .withDockerHost("unix:///var/run/docker.sock")

                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}