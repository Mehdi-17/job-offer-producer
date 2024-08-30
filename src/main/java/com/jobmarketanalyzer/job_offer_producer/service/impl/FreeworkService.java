package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import com.jobmarketanalyzer.job_offer_producer.service.JobScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeworkService implements FetchService, JobScraper {
    @Override
    public CompletableFuture<JobOffersDTO> fetchData() {
        //TODO: to implement
        return null;
    }

    @Override
    public Set<JobOffer> scrapeJobOffer() {
        //TODO to implement
        return null;
    }
}
