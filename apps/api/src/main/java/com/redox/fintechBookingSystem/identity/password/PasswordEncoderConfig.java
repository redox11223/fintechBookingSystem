package com.redox.fintechBookingSystem.identity.password;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

//this tells Spring that this class contains bean definitions and that it should not
//use CGLIB proxies for method calls within this class, which can improve performance and
//reduce memory usage,just remember to not call methods within this class that are annotated
//with @Bean from other methods in the same class, as this can lead to unexpected behavior.
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {
  @Bean
  public PasswordEncoder passwordEncoder(IdentityProperties properties){
    return new BCryptPasswordEncoder(properties.password().bcryptStrength());
  }
}
