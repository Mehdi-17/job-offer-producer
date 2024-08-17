package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
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

import java.util.List;
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
    public CompletableFuture<List<JobOffer>> fetchDataFromApi() {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.info("Call to France Travail ...");
        //todo : concate criteria -> typeContrat=LIB&motsCles=Java
        ResponseEntity<String> response = restTemplate.exchange(franceTravailApiUrl+"typeContrat=LIB&motsCles=Java",
                HttpMethod.GET,
                entity,
                String.class);

        log.info("France Travail : " + response);
        return CompletableFuture.completedFuture(List.of());
    }

    private String getAccessToken(){
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

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Access Token successfully retrieved.");

            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } catch (Exception e){
                log.error("Error parsing response body: {}", e.getMessage());
            }

        } else {
            log.error("Failed to retrieve access token. Status code: {}", response.getStatusCode());
            throw new RuntimeException("Failed to obtain access token: " + response.getStatusCode());
        }
        return null;
    }

}
