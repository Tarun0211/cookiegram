package com.cookigram.config;

import com.cookigram.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        
        String redirectUrl;
        if (roles.contains(Role.ROLE_ADMIN.name())) {
            redirectUrl = "/admin/dashboard";
        } else if (roles.contains(Role.ROLE_EMPLOYEE.name())) {
            redirectUrl = "/employee/dashboard";
        } else if (roles.contains(Role.ROLE_CUSTOMER.name())) {
            redirectUrl = "/customer/dashboard";
        } else {
            redirectUrl = "/";
        }
        
        response.sendRedirect(redirectUrl);
    }
}