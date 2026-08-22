package com.redox.fintechBookingSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles({"test", "prod"})
class OpenApiProductionIntegrationTests {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void doesNotExposeApiDocsInProduction() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isNotFound());
  }
}
