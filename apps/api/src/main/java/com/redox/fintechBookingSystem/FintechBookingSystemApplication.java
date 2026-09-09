package com.redox.fintechBookingSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
//this scans for @ConfigurationProperties annotated classes and
//registers them as beans in the Spring context
@ConfigurationPropertiesScan
public class FintechBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintechBookingSystemApplication.class, args);
	}

}
