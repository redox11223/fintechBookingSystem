package com.redox.fintechBookingSystem.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Returns Problem Details with 404 when the resource does not exist")
  void shouldReturn404WhenResourceNotFound() throws Exception {
    mockMvc.perform(get("/test/resource-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.citafin.dev/problems/resource-not-found"))
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.instance").value("/test/resource-not-found"))
            .andExpect(jsonPath("$.detail").value("Resource not found"));
  }

  @Test
  @DisplayName("Returns Problem Details with 409 when the resource is duplicated")
  void shouldReturn409WhenDuplicateResource() throws Exception {
    mockMvc.perform(post("/test/duplicate-resource")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.citafin.dev/problems/duplicate-resource"))
            .andExpect(jsonPath("$.title").value("Duplicate resource"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
            .andExpect(jsonPath("$.instance").value("/test/duplicate-resource"))
            .andExpect(jsonPath("$.detail").value("Duplicate resource"));
  }

  @Test
  @DisplayName("Returns Problem Details with 400 when request validation fails")
  void shouldReturn400WhenInvalidRequest() throws Exception {
    mockMvc.perform(post("/test/valid-request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name": ""}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.citafin.dev/problems/validation-error"))
            .andExpect(jsonPath("$.title").value("Validation error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.instance").value("/test/valid-request"))
            .andExpect(jsonPath("$.detail").value("Request contained invalid fields"))
            .andExpect(jsonPath("$.errors.name", containsInAnyOrder(
                    "Name can't be empty",
                    "Name must have at least 3 characters")));
  }

  @RestController
  @RequestMapping("/test")
  static class TestController {

    @GetMapping("/resource-not-found")
    void getResourceNotFound() {
      throw new ResourceNotFoundException("Resource not found");
    }

    @PostMapping("/duplicate-resource")
    void postDuplicateResource() {
      throw new DuplicateResourceException("Duplicate resource");
    }

    @PostMapping("/valid-request")
    void postValidRequest(@RequestBody @Valid TestRequest request) {}
  }

  record TestRequest(
          @NotBlank(message = "Name can't be empty")
          @Size(min = 3, message = "Name must have at least 3 characters")
          String name
  ) {}
}
