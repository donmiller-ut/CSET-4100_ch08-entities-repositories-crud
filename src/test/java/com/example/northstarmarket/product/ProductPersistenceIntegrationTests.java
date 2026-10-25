package com.example.northstarmarket.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductPersistenceIntegrationTests {

	@Autowired
	private ProductRepository products;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductCatalog catalog;

	@Autowired
	private EntityManager entityManager;

	@Test
	void migrationsProvideTheSeededProductAndDefaultCategory() {
		ProductEntity product = products.findById(42L).orElseThrow();
		CategoryEntity category = categories.findById(1L).orElseThrow();

		assertThat(product.getName()).isEqualTo("Trail Map Set");
		assertThat(product.getCategoryId()).isEqualTo(category.getId());
		assertThat(category.getName()).isEqualTo("Maps and Navigation");
	}

	@Test
	void repositoryUsesPostgresqlGeneratedIdentity() {
		ProductEntity saved = products.saveAndFlush(new ProductEntity(
				"Repository Compass",
				"Created through the repository test",
				new BigDecimal("29.95"),
				true,
				1L));

		assertThat(saved.getId()).isGreaterThan(42L);
		assertThat(products.findById(saved.getId())).isPresent();
	}

	@Test
	void managedReplacementIsWrittenThroughDirtyChecking() {
		ProductReplacement replacement = new ProductReplacement(
				"Persistent Trail Map Set",
				null,
				new BigDecimal("19.75"),
				false);

		Product changed = catalog.replace(42L, replacement);
		entityManager.flush();
		entityManager.clear();

		ProductEntity reloaded = products.findById(42L).orElseThrow();
		assertThat(changed.name()).isEqualTo("Persistent Trail Map Set");
		assertThat(reloaded.getName()).isEqualTo("Persistent Trail Map Set");
		assertThat(reloaded.isActive()).isFalse();
	}

	@Test
	void databaseConstraintRejectsAnInvalidProductEvenWithoutDtoValidation() {
		ProductEntity invalid = new ProductEntity(
				"Invalid Database Price",
				null,
				new BigDecimal("0.00"),
				true,
				1L);

		assertThatThrownBy(() -> products.saveAndFlush(invalid))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}

