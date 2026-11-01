package com.mango.products.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mango.products.repository.CredentialsRepository;

@Service
public class AuthService {

    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    // Compared against when the user does not exist. Even if we know that we return false, we still do this call to avoid timing attacks that could reveal if the user exists or not
    private final String dummyHash;

    public AuthService(CredentialsRepository credentialsRepository, PasswordEncoder passwordEncoder) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-unknown-users");
    }

    public boolean authenticate(String username, String password) {
        return credentialsRepository.findByUsername(username)
                .map(credentials -> passwordEncoder.matches(password, credentials.getPasswordHash()))
                .orElseGet(() -> {
                    passwordEncoder.matches(password, dummyHash);
                    return false;
                });
    }
}
