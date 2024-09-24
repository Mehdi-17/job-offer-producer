package com.jobmarketanalyzer.job_offer_producer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;

import java.util.Set;

public class JsonUtils {

    public static String buildJson(Set<JobOffer> jobOffers, ObjectMapper objectMapper) throws JsonProcessingException {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (JobOffer jobOffer : jobOffers) {
            ObjectNode jobNode = objectMapper.createObjectNode();
            jobNode.put("title", jobOffer.title());
            jobNode.put("description", jobOffer.description());
            jobNode.put("dailyRate", jobOffer.dailyRate());
            jobNode.put("company", jobOffer.company());
            arrayNode.add(jobNode);
        }

        return objectMapper.writeValueAsString(arrayNode);
    }
}
