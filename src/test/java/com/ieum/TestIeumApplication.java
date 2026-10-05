package com.ieum;

import org.springframework.boot.SpringApplication;

public class TestIeumApplication {

	public static void main(String[] args) {
		SpringApplication.from(IeumApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
