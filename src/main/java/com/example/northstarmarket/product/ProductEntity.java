package com.example.northstarmarket.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product", schema = "northstar")
class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	protected ProductEntity() {
	}

	ProductEntity(String name, String description, BigDecimal price, boolean active, Long categoryId) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.categoryId = categoryId;
	}

	Long getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getDescription() {
		return description;
	}

	BigDecimal getPrice() {
		return price;
	}

	boolean isActive() {
		return active;
	}

	Long getCategoryId() {
		return categoryId;
	}

	void replace(String name, String description, BigDecimal price, boolean active) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
	}
}

