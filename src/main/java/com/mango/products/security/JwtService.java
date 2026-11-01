package com.mango.products.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class JwtService {

    static final int MIN_SECRET_BYTES = 32;
    private static final String ISSUER = "products-api";

    private final byte[] secret;
    private final Duration validity;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${app.security.jwt-secret:}") String secret,
            @Value("${app.security.token-validity:1h}") Duration validity) {
        this(secret, validity, Clock.systemUTC());
    }

    JwtService(String secret, Duration validity, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes (256 bits)");
        }
        this.secret = bytes;
        this.validity = validity;
        this.clock = clock;
    }

    public String generateToken(String username) {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(username)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validity)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the token", e);
        }
    }

    public long getValiditySeconds() {
        return validity.toSeconds();
    }

    // Validation and return of username from token (returns empty if invalid or expired)
    public Optional<String> validate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (!ISSUER.equals(claims.getIssuer()) || expiration == null
                    || !expiration.toInstant().isAfter(clock.instant())) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }
}
