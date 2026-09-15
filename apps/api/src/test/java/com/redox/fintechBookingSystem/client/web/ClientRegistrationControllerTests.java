package com.redox.fintechBookingSystem.client.web;

import com.redox.fintechBookingSystem.client.ClientRegistrationService;
import com.redox.fintechBookingSystem.client.dto.ClientRegistrationRequest;
import com.redox.fintechBookingSystem.shared.config.SecurityConfig;
import com.redox.fintechBookingSystem.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientRegistrationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClientRegistrationControllerTests {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private ClientRegistrationService registrationService;

  @Test
  void acceptsValidRegistrationWithoutCsrfToken() throws Exception {
    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "María Quispe",
                  "email": "client@example.com",
                  "password": "a sufficiently long password",
                  "phoneNumber": "+51987654321"
                }
                """))
        .andExpect(status().isAccepted())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Registration processed"));

    ArgumentCaptor<ClientRegistrationRequest> requestCaptor =
        ArgumentCaptor.forClass(ClientRegistrationRequest.class);
    verify(registrationService).registerClient(requestCaptor.capture());
    assertThat(requestCaptor.getValue().email()).isEqualTo("client@example.com");
  }

  @Test
  void rejectsStructurallyInvalidRegistrationAsProblemDetails() throws Exception {
    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName": "",
                  "email": "not-an-email",
                  "password": "",
                  "phoneNumber": "987654321"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.fullName").isArray())
        .andExpect(jsonPath("$.errors.email").isArray())
        .andExpect(jsonPath("$.errors.password").isArray())
        .andExpect(jsonPath("$.errors.phoneNumber").isArray());

    verifyNoInteractions(registrationService);
  }
}
