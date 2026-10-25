package com.example.northstarmarket.product;

public record PageLinksResponse(
		String self,
		String first,
		String previous,
		String next,
		String last) {
}
