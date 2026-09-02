package com.demo.teamboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeamboardApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamboardApiApplication.class, args);
	}

}
