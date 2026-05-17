package com.ilgiyebo.domain.review.config;

import com.ilgiyebo.domain.review.service.ExperienceGrantPort;
import com.ilgiyebo.domain.review.service.NoOpExperienceGrantPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewConfig {

    @Bean
    @ConditionalOnMissingBean(ExperienceGrantPort.class)
    public ExperienceGrantPort noOpExperienceGrantPort() {
        return new NoOpExperienceGrantPort();
    }
}
