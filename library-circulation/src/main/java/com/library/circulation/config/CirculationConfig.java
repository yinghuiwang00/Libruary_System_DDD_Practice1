package com.library.circulation.config;

import com.library.circulation.domain.model.CirculationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.library.circulation.domain.repository")
@EnableJpaAuditing
public class CirculationConfig {

    @Bean
    public CirculationPolicy circulationPolicy() {
        return CirculationPolicy.standard();
    }
}
