package com.redox.fintechBookingSystem.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

//the main purpose of a Clock is to simplify testing, when you use Instant.now() or LocalDateTime.now()
//you are asking the time directly to your OS, making unit tests harder because time never stops, you
// cant stop time or travel to the future to see what happens if a token expires, when you inject a clock
// you control the time
@Configuration(proxyBeanMethods = false)
public class TimeConfig {
  @Bean
  public Clock clock(){
    return Clock.systemUTC();
  }
}
