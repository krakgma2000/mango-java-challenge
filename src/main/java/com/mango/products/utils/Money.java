package com.mango.products.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    static final int DECIMALS = 2;

    private Money() {
    }

    public static BigDecimal format(BigDecimal value) {
        return value == null ? null : value.setScale(DECIMALS, RoundingMode.UNNECESSARY);
    }
}