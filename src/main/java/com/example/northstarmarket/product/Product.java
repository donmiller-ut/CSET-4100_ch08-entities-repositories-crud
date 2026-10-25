package com.example.northstarmarket.product;

import java.math.BigDecimal;

record Product(
		Long id,
		String name,
		String description,
		BigDecimal price,
		boolean active) {
}
