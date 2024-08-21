package com.jobmarketanalyzer.job_offer_producer.service;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    @Value("${kafka.topic.name}")
    private String topicName;

    private final KafkaTemplate<String, JobOffersDTO> kafkaTemplate;

    public void sendJobOffer(JobOffersDTO jobOffersDTO){
        if (jobOffersDTO == null){
            return;
        }

        log.info("Job offer from {} will be send to kafka.", jobOffersDTO.source());
        kafkaTemplate.send(topicName, jobOffersDTO);
    }
}
