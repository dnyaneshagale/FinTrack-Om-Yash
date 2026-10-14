package com.familyfinance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {}
    public static long toPaise(BigDecimal rupees) {
        if (rupees == null || rupees.signum() <= 0 || rupees.scale() > 2) throw new IllegalArgumentException("Amount must be positive with at most two decimal places");
        return rupees.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }
}
