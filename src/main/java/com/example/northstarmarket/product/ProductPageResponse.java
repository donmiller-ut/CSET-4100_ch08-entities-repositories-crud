package com.example.northstarmarket.product;

import java.util.List;

public record ProductPageResponse(
		List<ProductResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages,
		PageLinksResponse links) {
}
