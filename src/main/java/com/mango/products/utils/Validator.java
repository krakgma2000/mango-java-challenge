package com.mango.products.utils;

import com.mango.products.exceptions.ValidationException;

public final class Validator {

    private Validator() {
        // Private constructor to prevent instantiation
    }

    public static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field  + " cannot be null");
        }
    }

    public static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
			throw new ValidationException(field + " cannot be null or blank");
		}
    }
}