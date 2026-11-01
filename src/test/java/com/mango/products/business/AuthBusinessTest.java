package com.mango.products.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mango.products.domain.LoginRequestDTO;
import com.mango.products.domain.LoginResponseDTO;
import com.mango.products.exceptions.UnauthorizedException;
import com.mango.products.exceptions.ValidationException;
import com.mango.products.security.JwtService;
import com.mango.products.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthBusinessTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthBusiness authBusiness;

    @Test
    void returnsABearerTokenForValidCredentials() {
        when(authService.authenticate("mango_test", "secret")).thenReturn(true);
        when(jwtService.generateToken("mango_test")).thenReturn("the-token");
        when(jwtService.getValiditySeconds()).thenReturn(3600L);

        LoginResponseDTO response = authBusiness.login(new LoginRequestDTO("mango_test", "secret"));

        assertThat(response.getAccessToken()).isEqualTo("the-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void wrongCredentialsAre401WithTheSameMessageForUsernameAndPassword() {
        when(authService.authenticate("mango_test", "wrong")).thenReturn(false);

        assertThatThrownBy(() -> authBusiness.login(new LoginRequestDTO("mango_test", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void aMissingUsernameIs400() {
        assertThatThrownBy(() -> authBusiness.login(new LoginRequestDTO(" ", "secret")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("username cannot be null or blank");
        verify(authService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void aMissingPasswordIs400() {
        assertThatThrownBy(() -> authBusiness.login(new LoginRequestDTO("mango_test", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("password cannot be null or blank");
    }
}
