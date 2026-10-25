package com.example.northstarmarket.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category", schema = "northstar")
class CategoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Long id;

	@Column(nullable = false, length = 80, unique = true)
	private String name;

	protected CategoryEntity() {
	}

	Long getId() {
		return id;
	}

	String getName() {
		return name;
	}
}

