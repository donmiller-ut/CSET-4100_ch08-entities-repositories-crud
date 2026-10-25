package com.example.northstarmarket.product;

interface ProductCatalog {

	ProductPage findAll(int page, int size, Boolean active);

	Product findById(long productId);

	Product create(NewProduct input);

	Product replace(long productId, ProductReplacement input);

	void delete(long productId);
}

