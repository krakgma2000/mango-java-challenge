package com.mango.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mango.products.entity.CredentialsEntity;
import com.mango.products.repository.CredentialsRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialsRepository credentialsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(credentialsRepository, passwordEncoder);
    }

    @Test
    void acceptsTheRightPassword() {
        when(credentialsRepository.findByUsername("mango_test"))
                .thenReturn(Optional.of(new CredentialsEntity(1L, "mango_test", "stored-hash")));
        when(passwordEncoder.matches("right", "stored-hash")).thenReturn(true);

        assertThat(authService.authenticate("mango_test", "right")).isTrue();
    }

    @Test
    void rejectsAWrongPassword() {
        when(credentialsRepository.findByUsername("mango_test"))
                .thenReturn(Optional.of(new CredentialsEntity(1L, "mango_test", "stored-hash")));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThat(authService.authenticate("mango_test", "wrong")).isFalse();
    }

    @Test
    void anUnknownUserStillPaysTheBcryptCost() {
        when(credentialsRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(authService.authenticate("nobody", "whatever")).isFalse();
        verify(passwordEncoder).matches(eq("whatever"), eq("dummy-hash"));
    }
}
