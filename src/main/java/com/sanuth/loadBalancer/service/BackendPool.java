package com.sanuth.loadBalancer.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.sanuth.loadBalancer.config.LoadBalancerProperties;
import com.sanuth.loadBalancer.model.BackendNode;

@Component
public class BackendPool {

    private final List<BackendNode> backends;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public BackendPool(LoadBalancerProperties properties) {
        this.backends = properties.getBackends().stream()
            .map(BackendNode::new)
            .toList();
    }

    public Optional<BackendNode> selectNextHealthyBackend() {
        if (backends.isEmpty()) {
            return Optional.empty();
        }

        int size = backends.size();
        for (int i = 0; i < size; i++) {
            int index = Math.floorMod(currentIndex.getAndIncrement(), size);
            BackendNode backend = backends.get(index);
            if (backend.isHealthy()) {
                return Optional.of(backend);
            }
        }

        return Optional.empty();
    }

    public List<BackendNode> allBackends() {
        return Collections.unmodifiableList(backends);
    }
}
