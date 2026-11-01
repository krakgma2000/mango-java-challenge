package com.mango.products.utils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}