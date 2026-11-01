package com.mango.products.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.products.security.AuthenticationFilter;
import com.mango.products.security.GrantRegistry;
import com.mango.products.security.JwtService;

// Authentication without Spring Security: means one filter and a BCrypt encoder.
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilter(JwtService jwtService,
            GrantRegistry grantRegistry, ObjectMapper objectMapper) {
        FilterRegistrationBean<AuthenticationFilter> registration =
                new FilterRegistrationBean<>(new AuthenticationFilter(jwtService, grantRegistry, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}