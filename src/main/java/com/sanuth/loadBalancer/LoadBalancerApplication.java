package com.sanuth.loadBalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class LoadBalancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancerApplication.class, args);
    }
}
