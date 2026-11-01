package com.mango.products.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import com.mango.products.exceptions.ValidationException;

public final class Validator {

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

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

    public static void requireMaxLength(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(field + " cannot be longer than " + maxLength + " characters");
        }
    }

    public static void requireNoNullCharacter(String value, String field) {
        if (value != null && value.indexOf('\u0000') >= 0) {
            throw new ValidationException(field + " cannot contain the NUL character");
        }
    }

    public static void requireNotNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
    }

    public static void requireLowerThan(BigDecimal value, BigDecimal limit, String field) {
        if (value != null && value.compareTo(limit) >= 0) {
            throw new ValidationException(field + " must be lower than " + limit.toPlainString());
        }
    }

    public static void requireMaxDecimals(BigDecimal value, int maxDecimals, String field) {
        if (value != null && value.stripTrailingZeros().scale() > maxDecimals) {
            throw new ValidationException(field + " cannot have more than " + maxDecimals + " decimals");
        }
    }

    public static void requireCurrencyCode(String value, String field) {
        if (value != null && !CURRENCY_CODE.matcher(value).matches()) {
            throw new ValidationException(field + " must be 3 uppercase letters");
        }
    }

    public static void requireBefore(LocalDate start, LocalDate end, String startField, String endField) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ValidationException(startField + " must be before " + endField);
        }
    }

    public static void requireDateBetween(LocalDate value, LocalDate min, LocalDate max, String field) {
        if (value != null && (value.isBefore(min) || value.isAfter(max))) {
            throw new ValidationException(field + " must be between " + min + " and " + max);
        }
    }
}