package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.verification.EmailVerificationConfirmationService;
import com.redox.fintechBookingSystem.identity.verification.EmailResendService;
import com.redox.fintechBookingSystem.identity.verification.InvalidEmailVerificationTokenException;
import com.redox.fintechBookingSystem.shared.config.SecurityConfig;
import com.redox.fintechBookingSystem.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailVerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, IdentityExceptionHandler.class})
class EmailVerificationControllerTests {
  private static final String RAW_TOKEN = "raw-verification-token";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private EmailVerificationConfirmationService confirmationService;
  @MockitoBean private EmailResendService resendService;

  @Test
  void confirmsValidTokenWithoutCsrfAndReturnsNoContent() throws Exception {
    mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"token": "raw-verification-token"}
                """))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(confirmationService).confirm(RAW_TOKEN);
  }

  @Test
  void mapsInvalidTokenToStableProblemDetails() throws Exception {
    doThrow(new InvalidEmailVerificationTokenException())
        .when(confirmationService).confirm(RAW_TOKEN);

    mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"token": "raw-verification-token"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type")
            .value("https://api.citafin.dev/problems/invalid-email-verification-token"))
        .andExpect(jsonPath("$.title").value("Invalid email verification token"))
        .andExpect(jsonPath("$.detail").value("Email verification token is invalid"))
        .andExpect(jsonPath("$.instance")
            .value("/api/v1/auth/email-verifications/confirm"))
        .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOKEN_INVALID"));
  }

  @Test
  void rejectsBlankTokenAsValidationProblemBeforeCallingService() throws Exception {
    mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"token": ""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.token").isArray());

    verifyNoInteractions(confirmationService);
  }

  @Test
  void acceptsResendWithoutCsrfAndReturnsGenericResponse() throws Exception {
    mockMvc.perform(post("/api/v1/auth/email-verifications/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "CLIENT@example.com"}
                """))
        .andExpect(status().isAccepted())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Verification Email request accepted"));

    verify(resendService).resend("CLIENT@example.com");
  }

  @Test
  void rejectsInvalidResendEmailBeforeCallingService() throws Exception {
    mockMvc.perform(post("/api/v1/auth/email-verifications/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.email").isArray());

    verifyNoInteractions(resendService);
  }
}
