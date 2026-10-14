package com.familyfinance;

import com.familyfinance.domain.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test void convertsRupeesWithoutFloatingPoint() { assertEquals(12345, Money.toPaise(new BigDecimal("123.45"))); }
    @Test void rejectsMoreThanTwoDecimals() { assertThrows(IllegalArgumentException.class, () -> Money.toPaise(new BigDecimal("1.001"))); }
}
