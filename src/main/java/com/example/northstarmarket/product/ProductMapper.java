package com.example.northstarmarket.product;

import org.springframework.stereotype.Component;

@Component
class ProductMapper {

	NewProduct toNewProduct(ProductCreateRequest request) {
		return new NewProduct(
				request.getName(),
				request.getDescription(),
				request.getPrice(),
				request.isActive());
	}

	ProductReplacement toReplacement(ProductReplaceRequest request) {
		return new ProductReplacement(
				request.getName(),
				request.getDescription(),
				request.getPrice(),
				request.getActive());
	}

	ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.id(),
				product.name(),
				product.description(),
				product.price(),
				product.active());
	}

	ProductPageResponse toResponse(ProductPage page) {
		PageLinks links = page.links();
		return new ProductPageResponse(
				page.items().stream().map(this::toResponse).toList(),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages(),
				new PageLinksResponse(
						links.self(),
						links.first(),
						links.prev(),
						links.next(),
						links.last()));
	}
}
