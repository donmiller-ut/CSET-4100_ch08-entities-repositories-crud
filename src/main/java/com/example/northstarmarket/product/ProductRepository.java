package com.example.northstarmarket.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	Page<ProductEntity> findByActive(boolean active, Pageable pageable);

	long countByActive(boolean active);
}

