package com.portal.listener;

import com.portal.domain.User;
import com.portal.domain.UserPrincipal;
import com.portal.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {
    private final LoginAttemptService loginAttemptService;

    public AuthenticationSuccessListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // principal will be an entire user when signed in successfully
        Object principal = event.getAuthentication().getPrincipal();

        if(principal instanceof UserPrincipal){
            UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();
            // Remove user from catch since we signed in successfully
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }
}
