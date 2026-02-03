package com.sanuth.loadBalancer.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanuth.loadBalancer.exception.BackendProxyException;
import com.sanuth.loadBalancer.exception.NoHealthyBackendException;
import com.sanuth.loadBalancer.service.ProxyService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LoadBalancerController {

    private final ProxyService proxyService;

    public LoadBalancerController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/lb/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyService.forward(request, body);
    }

    @ExceptionHandler(NoHealthyBackendException.class)
    public ResponseEntity<String> handleNoHealthyBackend(NoHealthyBackendException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

    @ExceptionHandler(BackendProxyException.class)
    public ResponseEntity<String> handleProxyFailure(BackendProxyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
    }
}
