package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class IndeedService implements FetchService {

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchDataFromApi() {
        System.out.println("Indeed Thread : " + Thread.currentThread().getName());
        return null;
    }
}
