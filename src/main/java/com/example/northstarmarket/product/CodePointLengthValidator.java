package com.example.northstarmarket.product;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class CodePointLengthValidator implements ConstraintValidator<CodePointLength, String> {
    private int maximum;

    @Override
    public void initialize(CodePointLength constraint) {
        maximum = constraint.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.codePointCount(0, value.length()) <= maximum;
    }
}
