package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.DTO.JobOffersDTO;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Service
@AllArgsConstructor
public class JobOfferService {

    private final KafkaProducerService kafkaProducerService;
    private final FetchService franceTravailService;
    private final FetchService indeedService;
    private final FetchService freeworkService;

    //@Scheduled(cron = "${cron.expression}", zone = "Europe/Paris")
    @Scheduled(fixedRate = 50000000) //For development
    public void fetchOffer(){
        CompletableFuture<JobOffersDTO> franceTravailOffersFuture = franceTravailService.fetchData();
        CompletableFuture<JobOffersDTO> freeworkOffersFuture = freeworkService.fetchData();
        CompletableFuture<JobOffersDTO> indeedOffersFuture = indeedService.fetchData();

        //todo voir comment on vérifie que les futurs sont bon
        franceTravailOffersFuture.thenAccept(kafkaProducerService::sendJobOffer);
        indeedOffersFuture.thenAccept(kafkaProducerService::sendJobOffer);
        freeworkOffersFuture.thenAccept(kafkaProducerService::sendJobOffer);
    }
}
