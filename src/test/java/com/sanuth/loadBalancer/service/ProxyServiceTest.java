package com.sanuth.loadBalancer.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.sanuth.loadBalancer.config.LoadBalancerProperties;
import com.sanuth.loadBalancer.config.LoadBalancerProperties.BackendDefinition;
import com.sanuth.loadBalancer.exception.BackendProxyException;
import com.sanuth.loadBalancer.exception.NoHealthyBackendException;

class ProxyServiceTest {

    private ProxyService proxyService;
    private BackendPool backendPool;
    private FakeHttpClient fakeHttpClient;

    @BeforeEach
    void setUp() {
        LoadBalancerProperties properties = new LoadBalancerProperties();
        properties.setRequestTimeoutMs(3000);
        properties.getBackends().add(backend("backend-1", 8081));
        properties.getBackends().add(backend("backend-2", 8082));

        backendPool = new BackendPool(properties);
        fakeHttpClient = new FakeHttpClient();
        proxyService = new ProxyService(backendPool, fakeHttpClient, properties);
    }

    @Test
    void forwardsRequestsUsingRoundRobin() {
        AtomicInteger callCount = new AtomicInteger(0);
        fakeHttpClient.setResponder(request -> {
            int currentCall = callCount.getAndIncrement();

            assertEquals("/echo", request.uri().getPath());
            if (currentCall == 0) {
                assertEquals(8081, request.uri().getPort());
                return new FakeHttpResponse(200, "backend-1".getBytes(StandardCharsets.UTF_8), request);
            }
            assertEquals(8082, request.uri().getPort());
            return new FakeHttpResponse(200, "backend-2".getBytes(StandardCharsets.UTF_8), request);
        });

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/echo");

        ResponseEntity<byte[]> first = proxyService.forward(request, null);
        ResponseEntity<byte[]> second = proxyService.forward(request, null);

        assertEquals(200, first.getStatusCode().value());
        assertEquals(200, second.getStatusCode().value());
        assertArrayEquals("backend-1".getBytes(StandardCharsets.UTF_8), first.getBody());
        assertArrayEquals("backend-2".getBytes(StandardCharsets.UTF_8), second.getBody());
    }

    @Test
    void throwsWhenNoHealthyBackendExists() {
        backendPool.allBackends().forEach(backend -> backend.setHealthy(false));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/echo");

        assertThrows(NoHealthyBackendException.class, () -> proxyService.forward(request, null));
    }

    @Test
    void throwsProxyExceptionWhenBackendCallFails() {
        fakeHttpClient.setFailure(new IOException("connection failed"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/echo");

        assertThrows(BackendProxyException.class, () -> proxyService.forward(request, null));
    }

    private BackendDefinition backend(String host, int port) {
        BackendDefinition backend = new BackendDefinition();
        backend.setHost(host);
        backend.setPort(port);
        backend.setScheme("http");
        backend.setWeight(1);
        return backend;
    }

    private static class FakeHttpClient extends HttpClient {
        private java.util.function.Function<HttpRequest, HttpResponse<byte[]>> responder;
        private IOException failure;

        void setResponder(java.util.function.Function<HttpRequest, HttpResponse<byte[]>> responder) {
            this.responder = responder;
            this.failure = null;
        }

        void setFailure(IOException failure) {
            this.failure = failure;
            this.responder = null;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
            throws IOException {
            if (failure != null) {
                throw failure;
            }
            if (responder == null) {
                throw new IllegalStateException("No responder configured");
            }
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) responder.apply(request);
            return response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
            try {
                return CompletableFuture.completedFuture(send(request, responseBodyHandler));
            } catch (IOException ex) {
                return CompletableFuture.failedFuture(ex);
            }
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler,
            PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.of(ProxySelector.of(new InetSocketAddress("localhost", 0)));
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private static class FakeHttpResponse implements HttpResponse<byte[]> {
        private final int status;
        private final byte[] body;
        private final HttpRequest request;

        FakeHttpResponse(int status, byte[] body, HttpRequest request) {
            this.status = status;
            this.body = body;
            this.request = request;
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<byte[]>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (k, v) -> true);
        }

        @Override
        public byte[] body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
