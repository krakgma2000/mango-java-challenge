package com.mango.products.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mango.products.entity.UserGrantEntity;

class GrantRegistryTest {

    private final GrantRegistry registry = new GrantRegistry(List.of(
            new UserGrantEntity("mango_test", "/products/**"),
            new UserGrantEntity("reader", "/products/prices/history")));

    @Test
    void theWildcardPatternCoversTheBasePathWithoutTrailingSlash() {
        // POST /products is the endpoint that creates products
        assertThat(registry.isGranted("mango_test", "/products")).isTrue();
    }

    @Test
    void theWildcardPatternCoversEverySubpath() {
        assertThat(registry.isGranted("mango_test", "/products/prices")).isTrue();
        assertThat(registry.isGranted("mango_test", "/products/prices/history")).isTrue();
    }

    @Test
    void aPatternDoesNotCoverOtherPaths() {
        assertThat(registry.isGranted("mango_test", "/admin")).isFalse();
        assertThat(registry.isGranted("mango_test", "/productsX")).isFalse();
    }

    @Test
    void anExactPatternCoversOnlyThatPath() {
        assertThat(registry.isGranted("reader", "/products/prices/history")).isTrue();
        assertThat(registry.isGranted("reader", "/products/prices")).isFalse();
    }

    @Test
    void aUserWithoutGrantsCannotCallAnything() {
        assertThat(registry.isGranted("nobody", "/products")).isFalse();
    }
}
