package com.deploymentanyinit.Oauth2.Controller;

import com.deploymentanyinit.Oauth2.Entities.AuthProvider;
import com.deploymentanyinit.Oauth2.Entities.Users;
import com.deploymentanyinit.Oauth2.Security.CustomOAuth2User;
import com.deploymentanyinit.Oauth2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HelloController {

    @GetMapping("/home")
    public ResponseEntity<String> sayHello(@AuthenticationPrincipal CustomOAuth2User user) {
        return ResponseEntity.ok("Hello OAuth2, " + user.getName() + "!");
    }
}