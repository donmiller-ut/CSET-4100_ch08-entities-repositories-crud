package com.example.northstarmarket.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTests {

	@Test
	void unexpectedFailuresReturnSafeCorrelatedProblemDetails() throws Exception {
		MockMvc mockMvc = MockMvcBuilders
				.standaloneSetup(new FailingController())
				.setControllerAdvice(new ApiExceptionHandler())
				.addFilters(new RequestIdFilter())
				.build();

		mockMvc.perform(get("/test/unexpected").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().string("X-Request-ID", matchesPattern("req-[0-9a-f]{32}")))
				.andExpect(jsonPath("$.code").value("internal_error"))
				.andExpect(jsonPath("$.detail").value("The server could not complete the request."))
				.andExpect(jsonPath("$.requestId", matchesPattern("req-[0-9a-f]{32}")))
				.andExpect(content().string(not(containsString("database-password"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))))
				.andExpect(content().string(not(containsString("stackTrace"))));
	}

	@RestController
	private static final class FailingController {

		@GetMapping("/test/unexpected")
		String fail() {
			throw new IllegalStateException("database-password=secret");
		}
	}
}
