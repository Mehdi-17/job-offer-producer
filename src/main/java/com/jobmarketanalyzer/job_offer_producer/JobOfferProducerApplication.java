package com.jobmarketanalyzer.job_offer_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class JobOfferProducerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobOfferProducerApplication.class, args);
	}

}
