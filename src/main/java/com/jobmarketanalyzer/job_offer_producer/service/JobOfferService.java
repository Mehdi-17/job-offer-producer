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

    //@Scheduled(cron = "${cron.expression}", zone = "Europe/Paris")
    @Scheduled(fixedRate = 5000) //For development
    public void fetchOffer(){
        CompletableFuture<JobOffersDTO> franceTravailOffersFuture = franceTravailService.fetchDataFromApi();
        indeedService.fetchDataFromApi();

        franceTravailOffersFuture.thenAccept(franceTravailOffers ->
                        System.out.println("Responses from France Travail")
        //TODO send response to kafka
                );
    }
}
