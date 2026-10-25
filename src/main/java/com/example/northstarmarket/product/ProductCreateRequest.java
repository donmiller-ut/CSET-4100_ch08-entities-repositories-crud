package com.example.northstarmarket.product;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ProductCreateRequest {

	@NotBlank
	@CodePointLength(max = 120)
	private String name;

	@CodePointLength(max = 1000)
	private String description;

	@NotNull
	@DecimalMin("0.01")
	@PriceIncrement
	private BigDecimal price;

	@Schema(defaultValue = "true")
	private boolean active = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
