package com.redox.fintechBookingSystem;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  @Test
  void verifiesModuleBoundaries() {
    ApplicationModules.of(FintechBookingSystemApplication.class).verify();
  }
}
