package com.mango.products.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private GrantRegistry grantRegistry;

    private AuthenticationFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(jwtService, grantRegistry, new ObjectMapper());
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private static MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    private boolean reachedTheController() {
        return chain.getRequest() != null;
    }

    @Test
    void publicPathsNeedNoToken() throws Exception {
        filter.doFilter(request("/auth/login", null), response, chain);
        assertThat(reachedTheController()).isTrue();

        chain = new MockFilterChain();
        filter.doFilter(request("/actuator/health", null), new MockHttpServletResponse(), chain);
        assertThat(reachedTheController()).isTrue();
        verify(jwtService, never()).validate(anyString());
    }

    @Test
    void aMissingTokenIs401WithWwwAuthenticate() throws Exception {
        filter.doFilter(request("/products", null), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("Missing bearer token");
        assertThat(reachedTheController()).isFalse();
    }

    @Test
    void anotherAuthenticationSchemeIs401() throws Exception {
        filter.doFilter(request("/products", "Basic dXNlcjpwYXNz"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(reachedTheController()).isFalse();
    }

    @Test
    void anInvalidOrExpiredTokenIs401() throws Exception {
        when(jwtService.validate("bad")).thenReturn(Optional.empty());

        filter.doFilter(request("/products", "Bearer bad"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"invalid_token\"");
        assertThat(response.getContentAsString()).contains("Invalid or expired token");
        assertThat(reachedTheController()).isFalse();
    }

    @Test
    void aValidTokenWithoutTheGrantIs403() throws Exception {
        when(jwtService.validate("good")).thenReturn(Optional.of("mango_test"));
        when(grantRegistry.isGranted("mango_test", "/admin")).thenReturn(false);

        filter.doFilter(request("/admin", "Bearer good"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(response.getContentAsString()).contains("Not allowed to access this endpoint");
        assertThat(reachedTheController()).isFalse();
    }

    @Test
    void aValidTokenWithTheGrantReachesTheController() throws Exception {
        when(jwtService.validate("good")).thenReturn(Optional.of("mango_test"));
        when(grantRegistry.isGranted("mango_test", "/products/prices")).thenReturn(true);

        filter.doFilter(request("/products/prices", "Bearer good"), response, chain);

        assertThat(reachedTheController()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
