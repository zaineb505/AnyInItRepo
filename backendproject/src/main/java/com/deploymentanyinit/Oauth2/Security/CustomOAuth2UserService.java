package com.deploymentanyinit.Oauth2.Security;

import com.deploymentanyinit.Oauth2.Entities.AuthProvider;
import com.deploymentanyinit.Oauth2.Entities.Users;
import com.deploymentanyinit.Oauth2.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {


    /*
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                OAuth2User user = super.loadUser(userRequest);

                // Safely access the attributes map
                Map<String, Object> attributes = new HashMap<>(user.getAttributes()); // Create a mutable map

                // Example: Check if the name attribute exists
                String name = (String) attributes.get("name");
                if (name == null) {
                    name = "Unknown";  // Provide a fallback if name is missing
                }

                // Handle other attributes similarly, if necessary
                String email = (String) attributes.get("email");
                if (email == null) {
                    email = "No email provided";  // Provide a fallback if email is missing
                }

                // Update the attributes map if necessary
                attributes.put("name", name);
                attributes.put("email", email);

                // Return the modified OAuth2User with the updated attributes
                return new DefaultOAuth2User(user.getAuthorities(), attributes, "name");
            }

    */
/*
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(user.getAttributes());

        // Extract access token
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // Fetch email from GitHub API
        String email = fetchGitHubEmail(accessToken);

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from GitHub OAuth2 provider");
        }

        attributes.put("email", email);
        return new DefaultOAuth2User(user.getAuthorities(), attributes, "email");
    }
   /* private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Get the provider name (GitHub, Google, etc.)
        String provider = userRequest.getClientRegistration().getClientName().toUpperCase();  // Convert to uppercase

        // Extract necessary user details (email, name, etc.)
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email is missing from OAuth2 response");
        }
        // Create a new user entity
        Users user = new Users();
        user.setEmail(email);
        user.setName(name);
        user.setProvider(AuthProvider.valueOf(provider)); // Now it's case-insensitive

        // Save user in the database
        userRepository.save(user);

        return oauth2User;
    }*/

    /*
    private String fetchGitHubEmail(String accessToken) {
        String emailUrl = "https://api.github.com/user/emails";

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        // Set up request
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            // Make the API call
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(emailUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

            // Check if the response body is not null
            if (response.getBody() != null) {
                for (Map<String, Object> emailEntry : response.getBody()) {
                    Boolean isPrimary = (Boolean) emailEntry.get("primary");
                    Boolean isVerified = (Boolean) emailEntry.get("verified");

                    // Check if the email is primary and verified
                    if (Boolean.TRUE.equals(isPrimary) && Boolean.TRUE.equals(isVerified)) {
                        return (String) emailEntry.get("email");
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            // Log the error details
            System.err.println("GitHub API error: " + e.getResponseBodyAsString());
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Headers: " + e.getResponseHeaders());
        }

        // Return null if no valid email found
        return null;
    }
}*/
/*
    private String fetchGitHubEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            for (Map<String, Object> emailData : response.getBody()) {
                if (Boolean.TRUE.equals(emailData.get("primary")) && Boolean.TRUE.equals(emailData.get("verified"))) {
                    return (String) emailData.get("email");
                }
            }
        }
        throw new OAuth2AuthenticationException("No verified email found");
    }
}*/
    @Service
    public class CustomOAuth2UserService extends DefaultOAuth2UserService {

        private final UserRepository userRepository;

        public CustomOAuth2UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            OAuth2User oauth2User = super.loadUser(userRequest);

            String providerId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oauth2User.getAttributes();

            // Extract user info in a way that makes them effectively final
            UserInfo userInfo = extractUserInfo(providerId, attributes);

            // Create or update user in database
            Users user = userRepository.findByEmail(userInfo.email)
                    .orElseGet(() -> new Users(
                            userInfo.name,
                            userInfo.email,
                            userInfo.imageUrl,
                            true,
                            AuthProvider.valueOf(providerId.toUpperCase()),
                            providerId
                    ));

            // Update user details
            user.setName(userInfo.name);
            user.setImageUrl(userInfo.imageUrl);
            user.setProvider(AuthProvider.valueOf(providerId.toUpperCase()));
            user.setProviderId(providerId);

            userRepository.save(user);

            return new CustomOAuth2User(oauth2User, userInfo.email, userInfo.name, userInfo.imageUrl);
        }
        // Helper method to extract and process user info
        private UserInfo extractUserInfo(String providerId, Map<String, Object> attributes) {
            String email, name, imageUrl, username;

            if ("github".equalsIgnoreCase(providerId)) {
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                imageUrl = (String) attributes.get("avatar_url");
                username = (String) attributes.get("login");
                if (email == null) {
                    email = username + "@users.noreply.github.com";
                }
                if (name == null) {
                    name = username;
                }
            }
            else if ("google".equalsIgnoreCase(providerId)) {
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                imageUrl = (String) attributes.get("picture");
                username = email;
            }
            else if ("gitlab".equalsIgnoreCase(providerId)) {
                // Traitement pour GitLab
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                imageUrl = (String) attributes.get("avatar_url");
                username = (String) attributes.get("username");

                if (name == null) {
                    name = username;
                }

            }else {
                throw new OAuth2AuthenticationException("Unsupported provider: " + providerId);
            }

            return new UserInfo(email, name, imageUrl);
        }

        private record UserInfo(String email, String name, String imageUrl) {}


    }