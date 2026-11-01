package com.mango.products.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private static JwtService serviceAt(Instant instant) {
        return new JwtService(SECRET, Duration.ofHours(1), Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void aTokenItIssuedIsValidAndCarriesTheUsername() {
        JwtService service = serviceAt(NOW);

        assertThat(service.validate(service.generateToken("mango_test"))).contains("mango_test");
    }

    @Test
    void aTokenIsValidUntilItExpires() {
        String token = serviceAt(NOW).generateToken("mango_test");

        assertThat(serviceAt(NOW.plus(Duration.ofMinutes(59))).validate(token)).contains("mango_test");
        assertThat(serviceAt(NOW.plus(Duration.ofHours(1))).validate(token)).isEmpty();
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-of-at-least-32-bytes!", Duration.ofHours(1),
                Clock.fixed(NOW, ZoneOffset.UTC)).generateToken("mango_test");

        assertThat(serviceAt(NOW).validate(token)).isEmpty();
    }

    @Test
    void aTamperedTokenIsRejected() {
        String token = serviceAt(NOW).generateToken("mango_test");
        String[] parts = token.split("\\.");
        String otherPayload = serviceAt(NOW).generateToken("admin").split("\\.")[1];

        assertThat(serviceAt(NOW).validate(parts[0] + "." + otherPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void anUnsignedTokenIsRejected() {
        // alg "none": a classic attack on JWT libraries
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = serviceAt(NOW).generateToken("mango_test").split("\\.")[1];

        assertThat(serviceAt(NOW).validate(header + "." + payload + ".")).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(serviceAt(NOW).validate("not-a-token")).isEmpty();
        assertThat(serviceAt(NOW).validate("")).isEmpty();
    }

    @Test
    void reportsTheValidityInSeconds() {
        assertThat(serviceAt(NOW).getValiditySeconds()).isEqualTo(3600);
    }

    @Test
    void doesNotStartWithoutASecret() {
        assertThatThrownBy(() -> new JwtService("", Duration.ofHours(1), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET is required");
        assertThatThrownBy(() -> new JwtService(null, Duration.ofHours(1), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void doesNotStartWithASecretShorterThan256Bits() {
        assertThatThrownBy(() -> new JwtService("0123456789abcdef0123456789abcde", Duration.ofHours(1), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes (256 bits)");
    }
}
