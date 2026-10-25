package com.example.northstarmarket.product;

import java.util.List;

record ProductPage(
		List<Product> items,
		int page,
		int size,
		long totalElements,
		int totalPages,
		PageLinks links) {
}
