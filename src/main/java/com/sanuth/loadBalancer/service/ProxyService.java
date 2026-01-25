package com.sanuth.loadBalancer.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sanuth.loadBalancer.config.LoadBalancerProperties;
import com.sanuth.loadBalancer.exception.BackendProxyException;
import com.sanuth.loadBalancer.exception.NoHealthyBackendException;
import com.sanuth.loadBalancer.model.BackendNode;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProxyService {

    private final BackendPool backendPool;
    private final HttpClient httpClient;
    private final LoadBalancerProperties properties;

    public ProxyService(BackendPool backendPool, HttpClient httpClient, LoadBalancerProperties properties) {
        this.backendPool = backendPool;
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public ResponseEntity<byte[]> forward(HttpServletRequest request, byte[] body) {
        BackendNode backend = backendPool.selectNextHealthyBackend()
            .orElseThrow(() -> new NoHealthyBackendException("No healthy backend available"));

        URI targetUri = buildTargetUri(backend.getBaseUrl(), request);
        HttpRequest.Builder outgoingRequestBuilder = HttpRequest.newBuilder()
            .uri(targetUri)
            .timeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
            .method(request.getMethod(), buildBody(body));

        copyRequestHeaders(request, outgoingRequestBuilder);

        try {
            HttpResponse<byte[]> backendResponse = httpClient.send(outgoingRequestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            HttpHeaders responseHeaders = new HttpHeaders();
            backendResponse.headers().map().forEach(responseHeaders::put);
            return ResponseEntity.status(HttpStatusCode.valueOf(backendResponse.statusCode()))
                .headers(responseHeaders)
                .body(backendResponse.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BackendProxyException("Failed to proxy request to backend", ex);
        }
    }

    private HttpRequest.BodyPublisher buildBody(byte[] body) {
        if (body == null || body.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private URI buildTargetUri(String baseUrl, HttpServletRequest request) {
        try {
            URI baseUri = new URI(baseUrl);
            return new URI(
                baseUri.getScheme(),
                null,
                baseUri.getHost(),
                baseUri.getPort(),
                request.getRequestURI(),
                request.getQueryString(),
                null
            );
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid backend URI", ex);
        }
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder requestBuilder) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (HttpHeaders.HOST.equalsIgnoreCase(headerName) || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                requestBuilder.header(headerName, values.nextElement());
            }
        }
    }
}
