package com.library.circulation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.library.circulation", "com.library.shared"})
public class CirculationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CirculationApplication.class, args);
    }
}
