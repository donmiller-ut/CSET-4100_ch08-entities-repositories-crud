package com.example.northstarmarket.product;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.northstarmarket.web.ApiExceptionHandler;
import com.example.northstarmarket.web.RequestIdFilter;

@WebMvcTest(ProductController.class)
@Import({InMemoryProductCatalog.class, ProductMapper.class, ApiExceptionHandler.class, RequestIdFilter.class})
class ProductControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InMemoryProductCatalog catalog;

	@BeforeEach
	void resetCatalog() {
		catalog.reset();
	}

	@Test
	void listsProductsUsingPagingAndActiveFilter() throws Exception {
		mockMvc.perform(get("/api/products")
				.param("page", "0")
				.param("size", "10")
				.param("active", "true")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(42))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.links.self")
						.value("/api/products?page=0&size=10&active=true"));

		mockMvc.perform(get("/api/products")
				.param("page", Integer.toString(Integer.MAX_VALUE))
				.param("size", "100")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void getsAnExistingProduct() throws Exception {
		mockMvc.perform(get("/api/products/42").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.name").value("Trail Map Set"))
				.andExpect(jsonPath("$.price").value(18.50))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void reportsAContractShapedErrorForAMissingProduct() throws Exception {
		mockMvc.perform(get("/api/products/999").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(header().string("X-Request-ID", matchesPattern("req-[0-9a-f]{32}")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("https://api.northstar.example/problems/product-not-found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("product_not_found"))
				.andExpect(jsonPath("$.detail").value("Product 999 was not found"))
				.andExpect(jsonPath("$.requestId", matchesPattern("req-[0-9a-f]{32}")));
	}

	@Test
	void createsAProductAndReturnsItsLocation() throws Exception {
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Insulated Bottle",
						  "description": "One-liter bottle",
						  "price": 24.95
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/products/43"))
				.andExpect(jsonPath("$.id").value(43))
				.andExpect(jsonPath("$.name").value("Insulated Bottle"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void replacesEveryMutableProductField() throws Exception {
		mockMvc.perform(put("/api/products/42")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Revised Trail Map Set",
						  "description": null,
						  "price": 19.25,
						  "active": false
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.name").value("Revised Trail Map Set"))
				.andExpect(jsonPath("$.description").doesNotExist())
				.andExpect(jsonPath("$.price").value(19.25))
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void deletesAProductAndThenReportsItMissing() throws Exception {
		mockMvc.perform(delete("/api/products/42"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(get("/api/products/42").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsUnknownFieldsInsteadOfMassAssigningThem() throws Exception {
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "id": 9000,
						  "name": "Injected Identifier",
						  "price": 4.50
						}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Null Active State",
						  "price": 4.50,
						  "active": null
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("malformed_request"));
	}

	@Test
	void rejectsIncompleteReplacementData() throws Exception {
		mockMvc.perform(put("/api/products/42")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Incomplete Replacement",
						  "description": null,
						  "price": 7.00
						}
						"""))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void distinguishesAnOmittedNullableFieldFromAnExplicitNull() throws Exception {
		mockMvc.perform(put("/api/products/42")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Missing Description Property",
						  "price": 7.00,
						  "active": true
						}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors[0].field").value("description"));
	}

	@Test
	void enforcesTheDocumentedMaximumLengthsAndRequiredPrice() throws Exception {
		String request = """
				{
				  "name": "%s",
				  "description": "%s"
				}
				""".formatted("n".repeat(121), "d".repeat(1001));

		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.errors", hasSize(3)))
				.andExpect(jsonPath("$.errors[0].field").value("description"))
				.andExpect(jsonPath("$.errors[1].field").value("name"))
				.andExpect(jsonPath("$.errors[2].field").value("price"));
	}

	@Test
	void reportsEveryInvalidBodyFieldWithoutExposingRejectedValues() throws Exception {
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "   ",
						  "description": "acceptable",
						  "price": 0.001
						}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.errors", hasSize(3)))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[1].field").value("price"))
				.andExpect(jsonPath("$.errors[2].field").value("price"))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("0.001"))));
	}

	@Test
	void rejectsMalformedJsonWithASafeProblemResponse() throws Exception {
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Broken\",\"price\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("malformed_request"))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Exception"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("stackTrace"))));
	}

	@Test
	void rejectsOutOfRangeQueryAndPathParameters() throws Exception {
		mockMvc.perform(get("/api/products").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_parameter"));

		mockMvc.perform(get("/api/products/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_parameter"));
	}

	@Test
	void rejectsAnUnsupportedRequestMediaType() throws Exception {
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.TEXT_PLAIN)
				.content("not-json"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void rejectsAnUnacceptableResponseMediaType() throws Exception {
		mockMvc.perform(get("/api/products/42").accept(MediaType.APPLICATION_XML))
				.andExpect(status().isNotAcceptable());
	}
}
