package com.finantialhub.finantialhubapi;

import org.springframework.boot.SpringApplication;

public class TestFinantialhubapiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinantialhubapiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
