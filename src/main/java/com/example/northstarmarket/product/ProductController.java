package com.example.northstarmarket.product;

import java.net.URI;

import com.example.northstarmarket.web.ApiProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
class ProductController {

	private final ProductCatalog catalog;
	private final ProductMapper mapper;

	ProductController(ProductCatalog catalog, ProductMapper mapper) {
		this.catalog = catalog;
		this.mapper = mapper;
	}

	@GetMapping
	@Operation(summary = "List products")
	ProductPageResponse listProducts(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) Boolean active) {
		return mapper.toResponse(catalog.findAll(page, size, active));
	}

	@GetMapping("/{productId}")
	@Operation(summary = "Get one product")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Product found"),
			@ApiResponse(responseCode = "404", description = "Product not found",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ApiProblem.class)))
	})
	ProductResponse getProduct(@PathVariable @Positive long productId) {
		return mapper.toResponse(catalog.findById(productId));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create a product")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Product created"),
			@ApiResponse(responseCode = "422", description = "Product data is not acceptable",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ApiProblem.class)))
	})
	ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest input) {
		Product created = catalog.create(mapper.toNewProduct(input));
		return ResponseEntity
				.created(URI.create("/api/products/" + created.id()))
				.body(mapper.toResponse(created));
	}

	@PutMapping(path = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Replace a product")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Product replaced"),
			@ApiResponse(responseCode = "404", description = "Product not found",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ApiProblem.class))),
			@ApiResponse(responseCode = "422", description = "Product data is not acceptable",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ApiProblem.class)))
	})
	ProductResponse replaceProduct(
			@PathVariable @Positive long productId,
			@Valid @RequestBody ProductReplaceRequest input) {
		return mapper.toResponse(catalog.replace(productId, mapper.toReplacement(input)));
	}

	@DeleteMapping("/{productId}")
	@Operation(summary = "Delete a product")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Product deleted"),
			@ApiResponse(responseCode = "404", description = "Product not found",
					content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ApiProblem.class)))
	})
	ResponseEntity<Void> deleteProduct(@PathVariable @Positive long productId) {
		catalog.delete(productId);
		return ResponseEntity.noContent().build();
	}
}
