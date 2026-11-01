package com.mango.products.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class AuthenticationFilter extends OncePerRequestFilter {

    // PUBLIC PATHS!
    static final List<String> PUBLIC_PATHS = List.of("/auth/login", "/actuator/health", "/actuator/health/**");

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final GrantRegistry grantRegistry;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(JwtService jwtService, GrantRegistry grantRegistry, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.grantRegistry = grantRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeError(response, request, HttpStatus.UNAUTHORIZED, "Bearer", "Missing bearer token");
            return;
        }

        Optional<String> username = jwtService.validate(header.substring(BEARER_PREFIX.length()));
        if (username.isEmpty()) {
            writeError(response, request, HttpStatus.UNAUTHORIZED, "Bearer error=\"invalid_token\"",
                    "Invalid or expired token");
            return;
        }

        if (!grantRegistry.isGranted(username.get(), request.getRequestURI())) {
            writeError(response, request, HttpStatus.FORBIDDEN, null, "Not allowed to access this endpoint");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
            String wwwAuthenticate, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (wwwAuthenticate != null) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        }
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
