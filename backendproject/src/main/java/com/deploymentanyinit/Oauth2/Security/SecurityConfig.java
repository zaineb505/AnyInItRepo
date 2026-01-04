package com.deploymentanyinit.Oauth2.Security;

import com.deploymentanyinit.Oauth2.Service.GitLabService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
    private final AuthenticationSuccessHandler successHandler;


    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          CustomOAuth2AuthenticationSuccessHandler successHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.successHandler = successHandler;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user", "/token").authenticated()
                        .requestMatchers("/api/auth/token").authenticated()
                        .requestMatchers("/api/auth/user").authenticated()
                        .requestMatchers("/api/github/**").authenticated()
                        .requestMatchers("/api/gitlab/**").authenticated()
                        .requestMatchers("/api/auth/gitlab/**").authenticated()
                        .requestMatchers("/api/docker/**").authenticated()
                        .anyRequest().permitAll()
                )

               .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(accessTokenResponseClient()))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))

                       .successHandler((request, response, authentication) -> {
                           OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                           String provider = oauthToken.getAuthorizedClientRegistrationId();

                           // Store the authentication without overwriting existing ones
                           request.getSession().setAttribute("OAUTH2_" + provider.toUpperCase(), oauthToken);

                           // Only update last provider if it's GitHub (for profile)
                           if ("github".equalsIgnoreCase(provider)) {
                               request.getSession().setAttribute("LAST_OAUTH_PROVIDER", provider);
                           }

                           // Redirect based on provider
                          /* String redirectUrl = "github".equals(provider)
                                   ? "http://localhost:4200/github-repos"
                                   : "gitlab".equals(provider)
                                   ? "http://localhost:4200/gitlab-ci"
                                   : "http://localhost:4200";

                           response.sendRedirect(redirectUrl);
                       })*/


                           String redirectParam = request.getParameter("redirect_to");

                           String redirectUrl = switch (provider.toLowerCase()) {
                               case "github" -> "http://localhost:4200/github-repos";
                               case "gitlab" -> redirectParam != null ? redirectParam : "http://localhost:4200/gitlab-ci";
                               default -> "http://localhost:4200";
                           };

                           response.sendRedirect(redirectUrl);
                       })
                )


                .logout(logout -> logout
                        .logoutSuccessUrl("http://localhost:4200")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                );


        return http.build();
    }

 @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        //config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        //config.setExposedHeaders(List.of("Authorization"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(githubClientRegistration(), this.gitLabClientRegistration()
                );

    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }



 @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }



    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}