package com.sanuth.loadBalancer.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient(LoadBalancerProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
            .build();
    }
}
