package com.example.northstarmarket.product;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@CompleteProductReplacement
public final class ProductReplaceRequest {

	@NotBlank
	@CodePointLength(max = 120)
	private String name;

	@CodePointLength(max = 1000)
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	private String description;
	private boolean descriptionProvided;

	@NotNull
	@DecimalMin("0.01")
	@PriceIncrement
	private BigDecimal price;

	@NotNull
	private Boolean active;

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
		this.descriptionProvided = true;
	}

	boolean isDescriptionProvided() {
		return descriptionProvided;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
