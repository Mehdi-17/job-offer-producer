package com.jobmarketanalyzer.job_offer_producer.model;

import lombok.Builder;

//todo rajouter ici et dans le consumer si une offre n'a pas de date associée, on mettra la date du scraping
//     rajouter la compagnie qui chercher
@Builder
public record JobOffer(String title, String description, String dailyRate) {
}
