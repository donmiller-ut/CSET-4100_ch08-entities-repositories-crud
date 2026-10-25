package com.example.northstarmarket.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaProductCatalog implements ProductCatalog {

	private static final long DEFAULT_CATEGORY_ID = 1L;

	private final ProductRepository products;

	JpaProductCatalog(ProductRepository products) {
		this.products = products;
	}

	@Override
	@Transactional(readOnly = true)
	public ProductPage findAll(int page, int size, Boolean active) {
		long requestedStart = (long) page * size;
		if (requestedStart > Integer.MAX_VALUE) {
			long totalElements = active == null ? products.count() : products.countByActive(active);
			return page(List.of(), page, size, totalElements, active);
		}

		PageRequest request = PageRequest.of(page, size, Sort.by("id"));
		Page<ProductEntity> result = active == null
				? products.findAll(request)
				: products.findByActive(active, request);
		return page(result.getContent(), page, size, result.getTotalElements(), active);
	}

	@Override
	@Transactional(readOnly = true)
	public Product findById(long productId) {
		return toProduct(required(productId));
	}

	@Override
	@Transactional
	public Product create(NewProduct input) {
		ProductEntity entity = new ProductEntity(
				input.name(),
				input.description(),
				input.price(),
				input.active(),
				DEFAULT_CATEGORY_ID);
		return toProduct(products.save(entity));
	}

	@Override
	@Transactional
	public Product replace(long productId, ProductReplacement input) {
		ProductEntity entity = required(productId);
		entity.replace(input.name(), input.description(), input.price(), input.active());
		return toProduct(entity);
	}

	@Override
	@Transactional
	public void delete(long productId) {
		products.delete(required(productId));
	}

	private ProductEntity required(long productId) {
		return products.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));
	}

	private ProductPage page(
			List<ProductEntity> entities,
			int page,
			int size,
			long totalElements,
			Boolean active) {
		int totalPages = (int) Math.ceil((double) totalElements / size);
		return new ProductPage(
				entities.stream().map(this::toProduct).toList(),
				page,
				size,
				totalElements,
				totalPages,
				links(page, size, totalPages, active));
	}

	private Product toProduct(ProductEntity entity) {
		return new Product(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getPrice(),
				entity.isActive());
	}

	private PageLinks links(int page, int size, int totalPages, Boolean active) {
		int lastPage = Math.max(totalPages - 1, 0);
		return new PageLinks(
				pageLink(page, size, active),
				pageLink(0, size, active),
				page > 0 ? pageLink(page - 1, size, active) : null,
				(long) page + 1 < totalPages ? pageLink(page + 1, size, active) : null,
				pageLink(lastPage, size, active));
	}

	private String pageLink(int page, int size, Boolean active) {
		String activeQuery = active == null ? "" : "&active=" + active;
		return "/api/products?page=" + page + "&size=" + size + activeQuery;
	}
}
