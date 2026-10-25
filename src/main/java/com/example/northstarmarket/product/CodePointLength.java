package com.example.northstarmarket.product;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** String length in Unicode code points, matching the JSON contract. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodePointLengthValidator.class)
public @interface CodePointLength {
    int max();
    String message() default "must contain at most {max} Unicode code points";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
