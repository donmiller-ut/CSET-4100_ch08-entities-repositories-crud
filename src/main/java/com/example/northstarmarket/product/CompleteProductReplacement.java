package com.example.northstarmarket.product;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CompleteProductReplacementValidator.class)
public @interface CompleteProductReplacement {
	String message() default "description must be supplied for complete replacement";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
