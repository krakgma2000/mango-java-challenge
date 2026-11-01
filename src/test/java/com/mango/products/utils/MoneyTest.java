package com.mango.products.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({ "99.9, 99.90", "100, 100.00", "0, 0.00", "99.99, 99.99", "99.990, 99.99", "1E+2, 100.00" })
    void formatsWithExactlyTwoDecimals(String input, String expected) {
        BigDecimal formatted = Money.format(new BigDecimal(input));

        assertThat(formatted.toPlainString()).isEqualTo(expected);
        assertThat(formatted.scale()).isEqualTo(2);
    }

    @Test
    void keepsNull() {
        assertThat(Money.format(null)).isNull();
    }

    @Test
    void neverRoundsSilently() {
        assertThatThrownBy(() -> Money.format(new BigDecimal("99.999"))).isInstanceOf(ArithmeticException.class);
    }
}