package com.sanuth.loadBalancer.model;

import java.util.concurrent.atomic.AtomicBoolean;

import com.sanuth.loadBalancer.config.LoadBalancerProperties.BackendDefinition;

public class BackendNode {

    private final BackendDefinition definition;
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public BackendNode(BackendDefinition definition) {
        this.definition = definition;
    }

    public String getHost() {
        return definition.getHost();
    }

    public int getPort() {
        return definition.getPort();
    }

    public int getWeight() {
        return definition.getWeight();
    }

    public String getBaseUrl() {
        return definition.baseUrl();
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public void setHealthy(boolean isHealthy) {
        healthy.set(isHealthy);
    }
}
