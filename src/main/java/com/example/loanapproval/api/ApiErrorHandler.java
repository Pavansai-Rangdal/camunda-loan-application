package com.example.loanapproval.api;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Shared onStatus() Camunda-error-forwarding helper (single ClientResponse-arg form only -
 * .onStatus() takes Function&lt;ClientResponse, Mono&lt;? extends Throwable&gt;&gt;, the two-arg
 * (HttpStatusCode, ClientResponse) form does not compile with Spring WebFlux).
 */
public final class ApiErrorHandler {

    private ApiErrorHandler() {
    }

    public static Mono<? extends Throwable> extractError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(
                        new RuntimeException("Camunda error " + response.statusCode().value() + ": " + body)));
    }
}
