package com.mango.products.business;

import org.springframework.stereotype.Component;

import com.mango.products.domain.LoginRequestDTO;
import com.mango.products.domain.LoginResponseDTO;
import com.mango.products.exceptions.UnauthorizedException;
import com.mango.products.security.JwtService;
import com.mango.products.service.AuthService;
import com.mango.products.utils.Validator;

@Component
public class AuthBusiness {

    static final String TOKEN_TYPE = "Bearer";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthBusiness(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Validator.requireNotBlank(request.getUsername(), "username");
        Validator.requireNotBlank(request.getPassword(), "password");

        if (!authService.authenticate(request.getUsername(), request.getPassword())) {
            throw new UnauthorizedException("Invalid username or password"); //Same message for both to avoid giving a hibnt
        }

        return new LoginResponseDTO(jwtService.generateToken(request.getUsername()), TOKEN_TYPE,
                jwtService.getValiditySeconds());
    }
}
