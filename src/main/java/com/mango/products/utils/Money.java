package com.mango.products.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Money {

    private static final Logger log = LoggerFactory.getLogger(Money.class);

    static final int DECIMALS = 2;

    private Money() {
    }

    public static BigDecimal format(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.stripTrailingZeros().scale() > DECIMALS) {
            log.warn("Amount {} has more than {} decimals, rounded to {}", value.toPlainString(), DECIMALS,
                    value.setScale(DECIMALS, RoundingMode.HALF_UP).toPlainString());
        }
        return value.setScale(DECIMALS, RoundingMode.HALF_UP);
    }
}