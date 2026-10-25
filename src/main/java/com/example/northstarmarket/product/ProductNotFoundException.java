package com.example.northstarmarket.product;

public final class ProductNotFoundException extends RuntimeException {

	ProductNotFoundException(long productId) {
		super("Product " + productId + " was not found");
	}
}
