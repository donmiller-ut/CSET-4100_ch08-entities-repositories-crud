package com.example.northstarmarket.product;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

final class CompleteProductReplacementValidator
		implements ConstraintValidator<CompleteProductReplacement, ProductReplaceRequest> {

	@Override
	public boolean isValid(ProductReplaceRequest request, ConstraintValidatorContext context) {
		if (request == null || request.isDescriptionProvided()) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(
				"description must be supplied for complete replacement")
				.addPropertyNode("description")
				.addConstraintViolation();
		return false;
	}
}
