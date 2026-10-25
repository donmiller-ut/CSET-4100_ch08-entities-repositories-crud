package com.example.northstarmarket.product;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PriceIncrementValidator.class)
public @interface PriceIncrement {
    String message() default "must be a multiple of 0.01";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
