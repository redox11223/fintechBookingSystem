package com.redox.fintechBookingSystem;

import org.springframework.boot.SpringApplication;

public class TestFintechBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(FintechBookingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
