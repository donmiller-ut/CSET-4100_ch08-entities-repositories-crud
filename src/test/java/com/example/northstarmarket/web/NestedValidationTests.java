package com.example.northstarmarket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

/** Complete runnable fixture for the chapter's nested DTO example, not a production endpoint. */
@WebMvcTest(NestedValidationTests.AppointmentController.class)
@Import({NestedValidationTests.AppointmentController.class, ApiExceptionHandler.class, RequestIdFilter.class})
class NestedValidationTests {
    @Autowired MockMvc mvc;

    record AppointmentRequest(@NotNull LocalDateTime beginsAt, @NotNull @Valid PatientRequest patient) {}
    record PatientRequest(@NotBlank String name, @NotBlank @Email String email) {}
    record WithoutCascade(@NotNull PatientRequest patient) {}

    @Test void rejectsMissingPatient() throws Exception {
        mvc.perform(post("/test/appointments").contentType("application/json")
                .content("{\"beginsAt\":\"2026-10-05T10:00:00\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("patient"));
    }

    @Test void rejectsInvalidNestedValuesAndAcceptsValidValues() throws Exception {
        mvc.perform(post("/test/appointments").contentType("application/json")
                .content("{\"beginsAt\":\"2026-10-05T10:00:00\",\"patient\":{\"name\":\" \",\"email\":\"invalid\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("patient.email"))
                .andExpect(jsonPath("$.errors[1].field").value("patient.name"));
        mvc.perform(post("/test/appointments").contentType("application/json")
                .content("{\"beginsAt\":\"2026-10-05T10:00:00\",\"patient\":{\"name\":\"Alex\",\"email\":\"alex@example.com\"}}"))
                .andExpect(status().isOk());
    }

    @Test void removingCascadeSkipsChildConstraints() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var patient = new PatientRequest(" ", "invalid");
            assertThat(validator.validate(new WithoutCascade(patient))).isEmpty();
            assertThat(validator.validate(new AppointmentRequest(LocalDateTime.of(2026, 10, 5, 10, 0), patient)))
                    .hasSize(2);
        }
    }

    @Test void invalidReturnValueIsAServerFailure() throws Exception {
        mvc.perform(get("/test/invalid-return"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.detail").value("The server could not complete the request."));
    }

    @RestController
    static class AppointmentController {
        @PostMapping("/test/appointments")
        String accept(@Valid @RequestBody AppointmentRequest request) { return "accepted"; }
        @GetMapping("/test/invalid-return")
        @Positive int invalidReturn() { return -1; }
    }
}
