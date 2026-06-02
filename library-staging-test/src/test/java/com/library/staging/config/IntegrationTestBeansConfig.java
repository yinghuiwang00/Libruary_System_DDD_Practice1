package com.library.staging.config;

import com.library.circulation.domain.model.CirculationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides beans that are normally defined in per-module config classes
 * which are excluded from the combined test context to avoid bean name conflicts.
 */
@Configuration
public class IntegrationTestBeansConfig {

    @Bean
    public CirculationPolicy circulationPolicy() {
        return CirculationPolicy.standard();
    }
}
