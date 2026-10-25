package com.example.northstarmarket.product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class InMemoryProductCatalog implements ProductCatalog {

	private final Map<Long, Product> products = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(43);

	InMemoryProductCatalog() {
		reset();
	}

	@Override
	public ProductPage findAll(int page, int size, Boolean active) {
		List<Product> matchingProducts = products.values().stream()
				.filter(product -> active == null || product.active() == active)
				.sorted(Comparator.comparing(Product::id))
				.toList();

		long totalElements = matchingProducts.size();
		int totalPages = (int) Math.ceil((double) totalElements / size);
		long requestedStart = (long) page * size;
		int fromIndex = (int) Math.min(requestedStart, matchingProducts.size());
		int toIndex = Math.min(fromIndex + size, matchingProducts.size());
		List<Product> items = matchingProducts.subList(fromIndex, toIndex);

		return new ProductPage(
				items,
				page,
				size,
				totalElements,
				totalPages,
				links(page, size, totalPages, active));
	}

	@Override
	public Product findById(long productId) {
		Product product = products.get(productId);
		if (product == null) {
			throw new ProductNotFoundException(productId);
		}
		return product;
	}

	@Override
	public Product create(NewProduct input) {
		long id = nextId.getAndIncrement();
		Product product = new Product(id, input.name(), input.description(), input.price(), input.active());
		products.put(id, product);
		return product;
	}

	@Override
	public Product replace(long productId, ProductReplacement input) {
		findById(productId);
		Product replacement = new Product(
				productId,
				input.name(),
				input.description(),
				input.price(),
				input.active());
		products.put(productId, replacement);
		return replacement;
	}

	@Override
	public void delete(long productId) {
		if (products.remove(productId) == null) {
			throw new ProductNotFoundException(productId);
		}
	}

	void reset() {
		products.clear();
		products.put(42L, new Product(
				42L,
				"Trail Map Set",
				"Water-resistant regional trail maps",
				new BigDecimal("18.50"),
				true));
		nextId.set(43);
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
