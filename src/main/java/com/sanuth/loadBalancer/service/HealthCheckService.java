package com.sanuth.loadBalancer.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sanuth.loadBalancer.config.LoadBalancerProperties;
import com.sanuth.loadBalancer.model.BackendNode;

@Component
public class HealthCheckService {

    private final BackendPool backendPool;
    private final HttpClient httpClient;
    private final LoadBalancerProperties properties;

    public HealthCheckService(BackendPool backendPool, HttpClient httpClient, LoadBalancerProperties properties) {
        this.backendPool = backendPool;
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${lb.health-check-interval-ms:5000}")
    public void refreshHealth() {
        for (BackendNode backend : backendPool.allBackends()) {
            backend.setHealthy(isBackendHealthy(backend));
        }
    }

    private boolean isBackendHealthy(BackendNode backend) {
        String healthPath = normalizePath(properties.getHealthCheckPath());
        URI uri = URI.create(backend.getBaseUrl() + healthPath);
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/health";
        }
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }
}
