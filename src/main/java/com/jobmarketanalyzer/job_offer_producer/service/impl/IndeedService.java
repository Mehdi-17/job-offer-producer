package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class IndeedService implements FetchService {

    @Override
    @Async
    public CompletableFuture<List<JobOffer>> fetchDataFromApi() {
        System.out.println("Indeed Thread : " + Thread.currentThread().getName());
        return null;
    }
}
