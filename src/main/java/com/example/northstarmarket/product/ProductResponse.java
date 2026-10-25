package com.example.northstarmarket.product;

import java.math.BigDecimal;

public record ProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		boolean active) {
}
