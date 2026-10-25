package com.example.northstarmarket.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class BoundaryRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired InMemoryProductCatalog catalog;

    @BeforeEach void reset() { catalog.reset(); }

    @Test void preservesAllowOnMethodRejection() throws Exception {
        mvc.perform(post("/api/products/42").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("PUT")))
                .andExpect(header().string("Allow", containsString("DELETE")))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test void generatesCorrelationInsteadOfReflectingClientHeader() throws Exception {
        for (String path : new String[] {"/api/products/42", "/api/products/999", "/api/products?size=101"}) {
            var response = mvc.perform(get(path).header("X-Request-ID", "untrusted-client-text"))
                    .andReturn().getResponse();
            String id = response.getHeader("X-Request-ID");
            assertThat(id).matches("req-[0-9a-f]{32}");
            assertThat(response.getContentAsString()).doesNotContain("untrusted-client-text");
            if (response.getStatus() >= 400) {
                assertThat(response.getContentAsString()).contains("\"requestId\":\"" + id + "\"");
            }
        }
    }

    @Test void rejectsInvalidReplacementWithoutChangingState() throws Exception {
        Product original = catalog.findById(42);
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \",\"description\":null,\"price\":0.001,\"active\":false}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors", hasSize(3)));
        assertThat(catalog.findById(42)).isEqualTo(original);
    }

    @Test void replacementRetainsUnicodeAndNumericValueRules() throws Exception {
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"description\":\"%s\",\"price\":1.2300,\"active\":false}"
                        .formatted("\uD83D\uDE80".repeat(120), "\uD83D\uDE80".repeat(1000))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(1.23));
    }

    @Test void rejectsOversizedUnicodeAndUnapprovedPresenceFlags() throws Exception {
        for (String body : new String[] {
                "{\"name\":\"%s\",\"price\":1}".formatted("\uD83D\uDE80".repeat(121)),
                "{\"name\":\"Good\",\"description\":\"%s\",\"price\":1}".formatted("\uD83D\uDE80".repeat(1001))}) {
            mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableContent());
        }
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Good\",\"descriptionProvided\":true,\"price\":1,\"active\":true}"))
                .andExpect(status().isBadRequest());
        assertThat(catalog.findById(42).name()).isEqualTo("Trail Map Set");
    }

    @Test void allRepresentativeMvcFailuresUseSafeProblemDetails() throws Exception {
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                get("/api/products/no-number"), get("/api/products?size=101"),
                get("/api/products/999"), get("/api/products/42").accept("application/xml"),
                post("/api/products").contentType("text/plain").content("secret-value"),
                post("/api/products").contentType("application/json").content("{\"id\":\"secret-value\"}"),
                post("/api/products").contentType("application/json").content("{\"name\":\"Good\"}")}) {
            var result = mvc.perform(request)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type", startsWith("https://api.northstar.example/problems/")))
                    .andExpect(jsonPath("$.title").isNotEmpty())
                    .andExpect(jsonPath("$.detail").isNotEmpty())
                    .andExpect(jsonPath("$.instance", startsWith("/api/products"))).andReturn();
            int status = result.getResponse().getStatus();
            assertThat(status).isBetween(400, 499);
            assertThat(result.getResponse().getContentAsString())
                    .contains("\"status\":" + status)
                    .contains("\"requestId\":\"" + result.getResponse().getHeader("X-Request-ID") + "\"")
                    .doesNotContain("secret-value", "stackTrace", "Exception", "java.lang");
        }
    }
}
