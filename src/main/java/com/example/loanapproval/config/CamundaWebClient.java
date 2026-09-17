package com.example.loanapproval.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Shared WebClient bean used by the REST API layer to call the Camunda v2 REST API
 * (search tasks, search/complete user tasks, start process instances, fetch variables).
 *
 * IMPORTANT: this bean is intentionally NOT named "camundaClient" - the Camunda Spring Boot
 * starter auto-registers a bean with that name and a duplicate name causes a startup conflict.
 */
@Configuration
public class CamundaWebClient {

    @Bean
    public WebClient camundaRestClient(@Value("${camunda-rest.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Explicit ObjectMapper bean so constructor-injected ObjectMapper usages (e.g. in
     * UserTaskController, to unwrap Camunda's double-serialized variable values) always
     * resolve, and so OffsetDateTime/Instant fields serialize as ISO-8601 strings rather
     * than timestamps.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
