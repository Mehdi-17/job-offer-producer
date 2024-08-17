package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.service.impl.FranceTravailService;
import com.jobmarketanalyzer.job_offer_producer.service.impl.IndeedService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@AllArgsConstructor
public class JobOfferService {

    private final FetchService franceTravailService;
    private final FetchService indeedService;

    @Scheduled(fixedRate = 14400000) //every 4 hours
    public void fetchOffer(){
        CompletableFuture<JobOffersDTO> franceTravailOffersFuture = franceTravailService.fetchDataFromApi();
        indeedService.fetchDataFromApi();

        franceTravailOffersFuture.thenAccept(franceTravailOffers ->
                        System.out.println("Responses from France Travail")
        //TODO send response to kafka
                );
    }
}
