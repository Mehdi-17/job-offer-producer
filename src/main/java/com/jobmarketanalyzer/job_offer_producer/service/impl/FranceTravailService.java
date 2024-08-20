package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.AuthenticationService;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
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

    private final AuthenticationService authenticationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchDataFromApi() {
        try {
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
        } catch (RestClientException e) {
            log.error("Error fetching data from France Travail: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private JobOffersDTO buildJobOffer(String body) {
        return new JobOffersDTO(SourceOffer.FRANCE_TRAVAIL.name(), extractJobOffersFromResponse(body));
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + authenticationService.getFranceTravailAccessToken());

        return headers;
    }

    private String buildApiUrl() {
        final String freelanceContract = "LIB";
        final String keywords = "Java";

        LocalDate yesterday = LocalDate.now().minusDays(1);
        ZonedDateTime startOfDay = yesterday.atStartOfDay(ZoneOffset.UTC);
        String startOfDayFormatted = startOfDay.format(DateTimeFormatter.ISO_INSTANT);
        ZonedDateTime endOfDay = yesterday.atTime(23, 59, 59).atZone(ZoneOffset.UTC);
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
            String resultField = "resultats";
            JsonNode jsonNode = objectMapper.readTree(responseJson);

            if (jsonNode.has(resultField)) {
                return jsonNode.get(resultField).toString();
            }

            log.warn("Field '{}' not found in the response.", resultField);
            return "[]";
        } catch (JsonProcessingException e) {
            log.error("Error when parsing JSON response : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
