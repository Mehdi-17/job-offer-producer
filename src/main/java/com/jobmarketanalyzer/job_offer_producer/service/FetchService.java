package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.DTO.JobOffersDTO;

import java.util.concurrent.CompletableFuture;

public interface FetchService {
    CompletableFuture<JobOffersDTO> fetchData();
}
