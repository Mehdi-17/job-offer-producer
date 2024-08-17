package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FetchService {
    public CompletableFuture<List<JobOffer>> fetchDataFromApi();
}
