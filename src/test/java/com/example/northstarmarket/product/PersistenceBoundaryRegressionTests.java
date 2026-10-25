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
class PersistenceBoundaryRegressionTests {
    @Autowired ProductCatalog catalog;
    @Autowired ProductRepository products;
    @Autowired EntityManager entityManager;

    @Test void storesCodePointLimitsAndTrailingZeroCents() {
        var saved = catalog.create(new NewProduct("🚀".repeat(120), "🚀".repeat(1000),
                new BigDecimal("1.2300"), true));
        entityManager.flush();
        entityManager.clear();
        var read = catalog.findById(saved.id());
        assertThat(read.name()).isEqualTo(saved.name());
        assertThat(read.description()).isEqualTo(saved.description());
        assertThat(read.price()).isEqualByComparingTo("1.23");
    }

    @Test void highPageHasNoOverflowedNextLink() {
        var page = catalog.findAll(Integer.MAX_VALUE, 100, null);
        assertThat(page.items()).isEmpty();
        assertThat(page.links().next()).isNull();
        assertThat(page.totalElements()).isPositive();
    }

    @Test void databaseRejectsFractionalCentWithoutDtoValidation() {
        assertThatThrownBy(() -> products.saveAndFlush(new ProductEntity(
                "Fractional cent", null, new BigDecimal("1.001"), true, 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void databaseRejectsMissingCategory() {
        assertThatThrownBy(() -> products.saveAndFlush(new ProductEntity(
                "Missing category", null, new BigDecimal("1.00"), true, Long.MAX_VALUE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void contextReusesIdentityAndClearDetachesTheInstance() {
        var first = products.findById(42L).orElseThrow();
        assertThat(products.findById(42L).orElseThrow()).isSameAs(first);
        entityManager.clear();
        assertThat(entityManager.contains(first)).isFalse();
        assertThat(products.findById(42L).orElseThrow()).isNotSameAs(first);
    }
}
