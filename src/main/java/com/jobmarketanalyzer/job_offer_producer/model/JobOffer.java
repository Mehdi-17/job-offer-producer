package com.jobmarketanalyzer.job_offer_producer.model;

import lombok.Builder;

@Builder
public record JobOffer(String title, String description, String dailyRate, String company) {
}
