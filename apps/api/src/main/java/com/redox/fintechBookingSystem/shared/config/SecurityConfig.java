package com.redox.fintechBookingSystem.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
    return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    pathPattern(HttpMethod.POST, "/api/v1/auth/register"),
                    pathPattern(HttpMethod.POST, "/api/v1/auth/login"),
                    pathPattern(HttpMethod.POST, "/api/v1/auth/email-verifications/confirm"),
                    pathPattern(HttpMethod.POST, "/api/v1/auth/email-verifications/resend")
                    ))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                    auth.anyRequest().permitAll())
            .oauth2ResourceServer(resourceServer->
                    resourceServer.jwt(jwt->
                            jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
  }

  @Bean
  public UrlBasedCorsConfigurationSource corsConfigurationSource(
      SecurityProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    // 1. EXTRACTION: Tells Spring to look into our custom "roles" claim array
    // instead of the default OAuth2 "scope" or "scp" claim.
    // It maps strings like ["ADMIN"] into Spring's memory format: "ROLE_ADMIN".
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    // 2. IDENTITY CREATION: Takes the extracted authorities from above and builds the
    // final JwtAuthenticationToken. This runs AFTER the decoder verifies the token math.
    // This token allows us to use @PreAuthorize("hasRole('ADMIN')") seamlessly.
    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return authenticationConverter;
  }
}
