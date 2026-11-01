package com.mango.products.utils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.mango.products.exceptions.ValidationException;

class ValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void requireNotBlankRejectsNullEmptyAndBlank(String value) {
        assertThatThrownBy(() -> Validator.requireNotBlank(value, "name"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("name cannot be null or blank");
    }

    @Test
    void requireNotBlankAcceptsText() {
        assertThatCode(() -> Validator.requireNotBlank("Sports Shoes", "name")).doesNotThrowAnyException();
    }

    @Test
    void requireNotNullRejectsNull() {
        assertThatThrownBy(() -> Validator.requireNotNull(null, "initDate"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("initDate cannot be null");
    }

    @Test
    void requireNotNullAcceptsAValue() {
        assertThatCode(() -> Validator.requireNotNull("2024-01-01", "initDate")).doesNotThrowAnyException();
    }

    @Test
    void requireMaxLengthAcceptsTheLimitAndNull() {
        assertThatCode(() -> Validator.requireMaxLength("abc", 3, "name")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requireMaxLength(null, 3, "name")).doesNotThrowAnyException();
    }

    @Test
    void requireMaxLengthRejectsLongerText() {
        assertThatThrownBy(() -> Validator.requireMaxLength("abcd", 3, "name"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("name cannot be longer than 3 characters");
    }

    @Test
    void requireNoNullCharacterRejectsNul() {
        assertThatThrownBy(() -> Validator.requireNoNullCharacter("a\u0000b", "name"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("name cannot contain the NUL character");
    }

    @Test
    void requireNoNullCharacterAcceptsTextAndNull() {
        assertThatCode(() -> Validator.requireNoNullCharacter("Sports Shoes", "name")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requireNoNullCharacter(null, "name")).doesNotThrowAnyException();
    }

    @Test
    void requireNotNegativeAcceptsZero() {
        assertThatCode(() -> Validator.requireNotNegative(BigDecimal.ZERO, "value")).doesNotThrowAnyException();
    }

    @Test
    void requireNotNegativeRejectsNegative() {
        assertThatThrownBy(() -> Validator.requireNotNegative(new BigDecimal("-0.01"), "value"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("value cannot be negative");
    }

    @Test
    void requireLowerThanRejectsTheLimit() {
        assertThatThrownBy(() -> Validator.requireLowerThan(new BigDecimal("100"), new BigDecimal("100"), "value"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("value must be lower than 100");
        assertThatCode(() -> Validator.requireLowerThan(new BigDecimal("99.99"), new BigDecimal("100"), "value"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "99", "99.9", "99.99", "99.990", "1E+2" })
    void requireMaxDecimalsAcceptsUpToTwoSignificantDecimals(String value) {
        assertThatCode(() -> Validator.requireMaxDecimals(new BigDecimal(value), 2, "value")).doesNotThrowAnyException();
    }

    @Test
    void requireMaxDecimalsRejectsThreeDecimals() {
        assertThatThrownBy(() -> Validator.requireMaxDecimals(new BigDecimal("99.999"), 2, "value"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("value cannot have more than 2 decimals");
    }

    @ParameterizedTest
    @ValueSource(strings = { "eur", "EU", "EURO", "E1R", "" })
    void requireCurrencyCodeRejectsAnythingButThreeUppercaseLetters(String value) {
        assertThatThrownBy(() -> Validator.requireCurrencyCode(value, "currency"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("currency must be 3 uppercase letters");
    }

    @Test
    void requireCurrencyCodeAcceptsACodeAndNull() {
        assertThatCode(() -> Validator.requireCurrencyCode("USD", "currency")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requireCurrencyCode(null, "currency")).doesNotThrowAnyException();
    }

    @Test
    void requireBeforeAcceptsAnOpenEnd() {
        assertThatCode(() -> Validator.requireBefore(LocalDate.of(2024, 1, 1), null, "initDate", "endDate"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireBeforeRejectsTheSameDayAndAnEarlierEnd() {
        LocalDate day = LocalDate.of(2024, 1, 1);
        assertThatThrownBy(() -> Validator.requireBefore(day, day, "initDate", "endDate"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("initDate must be before endDate");
        assertThatThrownBy(() -> Validator.requireBefore(day, day.minusDays(1), "initDate", "endDate"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireDateBetweenAcceptsTheLimitsAndNull() {
        LocalDate min = LocalDate.of(1900, 1, 1);
        LocalDate max = LocalDate.of(9999, 12, 31);
        assertThatCode(() -> Validator.requireDateBetween(min, min, max, "initDate")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requireDateBetween(max, min, max, "initDate")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requireDateBetween(null, min, max, "endDate")).doesNotThrowAnyException();
    }

    @Test
    void requireDateBetweenRejectsDatesOutsideTheRange() {
        LocalDate min = LocalDate.of(1900, 1, 1);
        LocalDate max = LocalDate.of(9999, 12, 31);
        assertThatThrownBy(() -> Validator.requireDateBetween(LocalDate.of(1899, 12, 31), min, max, "initDate"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("initDate must be between 1900-01-01 and 9999-12-31");
        assertThatThrownBy(() -> Validator.requireDateBetween(LocalDate.of(-5000, 1, 1), min, max, "initDate"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validator.requireDateBetween(LocalDate.of(10000, 1, 1), min, max, "endDate"))
                .isInstanceOf(ValidationException.class);
    }
}