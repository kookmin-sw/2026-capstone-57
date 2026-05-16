package com.ilgiyebo.domain.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

@Configuration
public class BedrockConfig {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Value("${aws.bedrock.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(timeoutSeconds))
                        .apiCallAttemptTimeout(Duration.ofSeconds(timeoutSeconds)))
                .build();
    }
}
