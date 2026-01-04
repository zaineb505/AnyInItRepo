package com.deploymentanyinit.Oauth2.Security;  // Correct your package if needed

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        // Custom logic after successful authentication
        System.out.println("Authentication Successful!");

        // Redirect to home page or wherever needed
        response.sendRedirect("http://localhost:8080/api/github");
    }
}
