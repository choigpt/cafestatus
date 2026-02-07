package com.example.cafestatus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CafeStatusApplication {

	public static void main(String[] args) {
		SpringApplication.run(CafeStatusApplication.class, args);
	}

}
