package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;

import java.util.Set;

public interface JobScraper {
    Set<JobOffer> scrapeJobOffer();
}
