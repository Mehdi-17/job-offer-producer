package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import com.jobmarketanalyzer.job_offer_producer.service.JobScraper;
import com.jobmarketanalyzer.job_offer_producer.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeworkService implements FetchService, JobScraper {

    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchData() {
        Set<JobOffer> jobOffers = scrapeJobOffer();

        if (jobOffers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return CompletableFuture.completedFuture(new JobOffersDTO(SourceOffer.FREEWORK.name(), JsonUtils.buildJson(jobOffers, objectMapper)));
        } catch (JsonProcessingException e) {
            log.error("Error when writing JSON string for Freework job.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Set<JobOffer> scrapeJobOffer() {
        //TODO to implement
        return null;
    }
}
