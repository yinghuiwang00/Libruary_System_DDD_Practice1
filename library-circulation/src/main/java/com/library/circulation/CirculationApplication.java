package com.library.circulation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class CirculationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CirculationApplication.class, args);
    }
}
