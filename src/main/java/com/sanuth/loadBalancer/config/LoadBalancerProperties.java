package com.sanuth.loadBalancer.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lb")
public class LoadBalancerProperties {

    private List<BackendDefinition> backends = new ArrayList<>();
    private String healthCheckPath = "/health";
    private long healthCheckIntervalMs = 5000;
    private long requestTimeoutMs = 5000;

    public List<BackendDefinition> getBackends() {
        return backends;
    }

    public void setBackends(List<BackendDefinition> backends) {
        this.backends = backends;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public void setHealthCheckPath(String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    public long getHealthCheckIntervalMs() {
        return healthCheckIntervalMs;
    }

    public void setHealthCheckIntervalMs(long healthCheckIntervalMs) {
        this.healthCheckIntervalMs = healthCheckIntervalMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public static class BackendDefinition {
        private String host;
        private int port;
        private String scheme = "http";
        private int weight = 1;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String baseUrl() {
            return scheme + "://" + host + ":" + port;
        }
    }
}
