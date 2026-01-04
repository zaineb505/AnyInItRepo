package com.deploymentanyinit.Oauth2.Security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
/*
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final String email;

    public CustomOAuth2User(OAuth2User oauth2User, String email) {
        this.oauth2User = oauth2User;
        this.email = (email != null) ? email : oauth2User.getName();  // Using getName() to retrieve the GitHub username

    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }

    public String getEmail() {
        return email;
    }
}*/
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final String email;
    private final String name;
    private final String imageUrl;

    public CustomOAuth2User(OAuth2User oauth2User, String email, String name, String imageUrl) {
        this.oauth2User = oauth2User;
        this.email = email;
        this.name = name;
        this.imageUrl = imageUrl;
    }


    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return email;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
