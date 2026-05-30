package com.library.patron;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.library.patron", "com.library.shared"})
@EnableJpaRepositories
public class PatronApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatronApplication.class, args);
    }
}
