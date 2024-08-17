package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FranceTravailService implements FetchService {

    @Value("${france.travail.api.url}")
    private String franceTravailApiUrl;

    @Value("${france.travail.access.token.url}")
    private String franceTravailAccessTokenUrl;

    @Value("${france.travail.client.id}")
    private String clientId;

    @Value("${france.travail.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchDataFromApi() {
        log.info("Call France Travail API ...");

        ResponseEntity<String> response = restTemplate.exchange(
                buildApiUrl(),
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                String.class
        );

        log.info("France Travail API answer with a status code : {}", response.getStatusCode());

        return switch (response.getStatusCode()) {
            case HttpStatus.OK -> CompletableFuture.completedFuture(buildJobOffer(response.getBody()));
            case HttpStatus.NO_CONTENT -> CompletableFuture.completedFuture(null);
            case HttpStatus.PARTIAL_CONTENT -> {
                log.warn("France Travail API response with partial content. May check the filters");
                yield CompletableFuture.completedFuture(buildJobOffer(response.getBody()));
            }
            default -> {
                log.warn("France Travail API response status unsupported: {}", response.getStatusCode());
                yield CompletableFuture.completedFuture(null);
            }
        };
    }

    private JobOffersDTO buildJobOffer(String body) {
        return new JobOffersDTO(SourceOffer.FRANCE_TRAVAIL.name(), extractJobOffersFromResponse(body));
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + getAccessToken());

        return headers;
    }

    private String buildApiUrl() {
        final String freelanceContract = "LIB";
        final String keywords = "Java";

        LocalDate today = LocalDate.now();
        ZonedDateTime startOfDay = today.atStartOfDay(ZoneOffset.UTC);
        String startOfDayFormatted = startOfDay.format(DateTimeFormatter.ISO_INSTANT);
        ZonedDateTime endOfDay = today.atTime(23, 59, 59).atZone(ZoneOffset.UTC);
        String endOfDayFormatted = endOfDay.format(DateTimeFormatter.ISO_INSTANT);

        return UriComponentsBuilder.fromHttpUrl(franceTravailApiUrl)
                .queryParam("typeContrat", freelanceContract)
                .queryParam("motsCles", keywords)
                .queryParam("minCreationDate", startOfDayFormatted)
                .queryParam("maxCreationDate", endOfDayFormatted)
                .build()
                .toUriString();
    }

    private String extractJobOffersFromResponse(String responseJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            return jsonNode.get("resultats").toString();
        } catch (JsonProcessingException e) {
            log.error("Error when parsing JSON response : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("scope", "o2dsoffre api_offresdemploiv2");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        log.info("Call to generate access token...");
        ResponseEntity<String> response = restTemplate.exchange(
                franceTravailAccessTokenUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (HttpStatus.OK.equals(response.getStatusCode())) {
            log.info("Access Token successfully retrieved.");

            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } catch (Exception e) {
                log.error("Error parsing response body: {}", e.getMessage());
            }

        } else {
            log.error("Failed to retrieve access token. Status code: {}", response.getStatusCode());
            throw new RuntimeException("Failed to obtain access token: " + response.getStatusCode());
        }
        return null;
    }

}
