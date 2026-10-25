package com.example.northstarmarket.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ProductController.class)
@Import({InMemoryProductCatalog.class, ProductMapper.class,
        com.example.northstarmarket.web.ApiExceptionHandler.class,
        com.example.northstarmarket.web.RequestIdFilter.class})
class ProductContractTests {
    @Autowired private MockMvc mvc;
    @Autowired private InMemoryProductCatalog catalog;

    @BeforeEach
    void resetCatalog() {
        catalog.reset();
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void acceptsContractLengthLimitsIncludingUnicodeCharacters() throws Exception {
        String name = "\uD83D\uDE80".repeat(120);
        create("""
                {"name":"%s","description":"%s","price":0.01}
                """.formatted(name, "d".repeat(1000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void rejectsValuesBeyondContractLengthLimits() throws Exception {
        create("""
                {"name":"%s","price":1}
                """.formatted("n".repeat(121))).andExpect(status().isUnprocessableContent());
        create("""
                {"name":"Valid","description":"%s","price":1}
                """.formatted("d".repeat(1001))).andExpect(status().isUnprocessableContent());
        create("""
                {"name":"","price":1}
                """).andExpect(status().isUnprocessableContent());
    }

    @Test
    void checksPriceValueRatherThanInsignificantTrailingZeros() throws Exception {
        for (String price : new String[] {"0", "-1", "0.001", "1.001"}) {
            create("{\"name\":\"Price check\",\"price\":" + price + "}")
                    .andExpect(status().isUnprocessableContent());
        }
        create("{\"name\":\"Price check\",\"price\":1.2300}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(1.23));
    }

    @Test
    void requiresDescriptionOnReplacementButAcceptsExplicitNull() throws Exception {
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\",\"price\":1,\"active\":true}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/products/42"))
                .andExpect(jsonPath("$.name").value("Trail Map Set"));
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\",\"description\":null,\"price\":1,\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void rejectsServerControlledPropertiesOnReplacementWithoutMutation() throws Exception {
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":9000,\"name\":\"Changed\",\"description\":null,\"price\":1,\"active\":true}"))
                .andExpect(status().isBadRequest());
        assertThat(catalog.findById(42).name()).isEqualTo("Trail Map Set");
    }

    @Test
    void rejectsInvalidIdentifiersAndPagingBeforeLookup() throws Exception {
        for (String path : new String[] {"/0", "/-1", "/not-a-number",
                "?page=-1", "?size=0", "?size=101", "?page=abc", "?active=unknown"}) {
            mvc.perform(get("/api/products" + path)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/products/42?productId=999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void rejectsWrongMethodWithoutChangingTheProduct() throws Exception {
        mvc.perform(post("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(catalog.findById(42).name()).isEqualTo("Trail Map Set");
    }

    @Test
    void rejectsMalformedAndMissingBodiesWithoutCreatingState() throws Exception {
        create("{\"name\":").andExpect(status().isBadRequest());
        create("").andExpect(status().isBadRequest());
        assertThat(catalog.findAll(0, 20, null).totalElements()).isEqualTo(1);
    }

    @Test
    void returnsCanonicalPageLinksWithoutOverflow() throws Exception {
        mvc.perform(get("/api/products?page=2147483647&size=100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.links.next").value(nullValue()))
                .andExpect(jsonPath("$.links.previous")
                        .value("/api/products?page=2147483646&size=100"))
                .andExpect(jsonPath("$.links.prev").doesNotHaveJsonPath());
        mvc.perform(get("/api/products?active=false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void missingReplacementAndRepeatedDeleteDoNotCreateProducts() throws Exception {
        mvc.perform(put("/api/products/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Missing\",\"description\":null,\"price\":1,\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product_not_found"));
        mvc.perform(delete("/api/products/42")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/products/42")).andExpect(status().isNotFound());
        assertThat(catalog.findAll(0, 20, null).totalElements()).isZero();
    }

    @Test
    void missingProductUsesTheSameFreshRequestIdInHeaderAndBody() throws Exception {
        String previous = null;
        for (int i = 0; i < 2; i++) {
            var result = mvc.perform(get("/api/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andReturn();
            String id = result.getResponse().getHeader("X-Request-ID");
            assertThat(id).matches("req-[0-9a-f]{32}").isNotEqualTo(previous);
            assertThat(result.getResponse().getContentAsString())
                    .contains("\"requestId\":\"" + id + "\"")
                    .doesNotContain("stackTrace", "java.lang", "InMemoryProductCatalog");
            previous = id;
        }
    }

    @Test
    void createDefaultsDoNotTurnExplicitFalseIntoTrue() throws Exception {
        create("{\"name\":\"Default\",\"price\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.active").value(true));
        create("{\"name\":\"Inactive\",\"price\":1,\"active\":false}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(false));
    }
}
