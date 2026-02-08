package com.sanuth.loadBalancer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sanuth.loadBalancer.config.LoadBalancerProperties;
import com.sanuth.loadBalancer.config.LoadBalancerProperties.BackendDefinition;

class BackendPoolTest {

    @Test
    void selectsBackendsInRoundRobinOrder() {
        BackendPool pool = new BackendPool(propertiesWithThreeBackends());

        String first = pool.selectNextHealthyBackend().orElseThrow().getHost();
        String second = pool.selectNextHealthyBackend().orElseThrow().getHost();
        String third = pool.selectNextHealthyBackend().orElseThrow().getHost();
        String fourth = pool.selectNextHealthyBackend().orElseThrow().getHost();

        assertEquals("backend-1", first);
        assertEquals("backend-2", second);
        assertEquals("backend-3", third);
        assertEquals("backend-1", fourth);
    }

    @Test
    void skipsUnhealthyBackends() {
        BackendPool pool = new BackendPool(propertiesWithThreeBackends());
        pool.allBackends().get(1).setHealthy(false);

        String first = pool.selectNextHealthyBackend().orElseThrow().getHost();
        String second = pool.selectNextHealthyBackend().orElseThrow().getHost();
        String third = pool.selectNextHealthyBackend().orElseThrow().getHost();

        assertEquals("backend-1", first);
        assertEquals("backend-3", second);
        assertEquals("backend-1", third);
    }

    @Test
    void returnsEmptyWhenAllBackendsAreUnhealthy() {
        BackendPool pool = new BackendPool(propertiesWithThreeBackends());
        pool.allBackends().forEach(backend -> backend.setHealthy(false));

        assertTrue(pool.selectNextHealthyBackend().isEmpty());
    }

    @Test
    void returnsEmptyWhenNoBackendsConfigured() {
        LoadBalancerProperties properties = new LoadBalancerProperties();
        BackendPool pool = new BackendPool(properties);

        assertFalse(pool.selectNextHealthyBackend().isPresent());
    }

    private LoadBalancerProperties propertiesWithThreeBackends() {
        LoadBalancerProperties properties = new LoadBalancerProperties();
        properties.getBackends().add(backend("backend-1", 8081));
        properties.getBackends().add(backend("backend-2", 8082));
        properties.getBackends().add(backend("backend-3", 8083));
        return properties;
    }

    private BackendDefinition backend(String host, int port) {
        BackendDefinition backend = new BackendDefinition();
        backend.setHost(host);
        backend.setPort(port);
        backend.setScheme("http");
        backend.setWeight(1);
        return backend;
    }
}
