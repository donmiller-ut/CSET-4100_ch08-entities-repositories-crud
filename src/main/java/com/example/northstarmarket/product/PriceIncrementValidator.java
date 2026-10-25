package com.example.northstarmarket.product;

import java.math.BigDecimal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class PriceIncrementValidator implements ConstraintValidator<PriceIncrement, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Null is handled separately by @NotNull; trailing zeros do not change value.
        return value == null || value.stripTrailingZeros().scale() <= 2;
    }
}
