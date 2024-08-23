package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndeedService implements FetchService {

    @Value("${indeed.search.url}")
    private String indeedSearchUrl;

    @Value("${indeed.job.elements}")
    private String indeedJobElementsName;

    private final WebDriver driver;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchData() {

            List<String> jobsText = scrapeJobOffer();

            if (jobsText.isEmpty()){
                return CompletableFuture.completedFuture(null);
            }

            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (String jobText : jobsText) {
                ObjectNode jobNode = objectMapper.createObjectNode();

                jobNode.put("job", jobText);
                arrayNode.add(jobNode);
            }

        try {
            String json = objectMapper.writeValueAsString(arrayNode);

            return CompletableFuture.completedFuture(new JobOffersDTO(SourceOffer.INDEED.name(), json));
        } catch (JsonProcessingException e) {
            log.error("Error when writing JSON string for Indeed job.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<String> scrapeJobOffer() {
        try {
            log.info("Start headless browser to scrape indeed job offers");
            driver.get(indeedSearchUrl);

            List<WebElement> elements = driver.findElements(By.cssSelector(indeedJobElementsName));
            log.info("Scrape {} offers on indeed", elements.size());

            //Todo voir pour récupérer plus de data

            //TODO: prendre en compte quand on a plusieurs pages de résultats.
            return elements.stream().map(WebElement::getText).toList();
        } finally {
            log.info("Quit headless browser");
            driver.quit();
        }
    }
}
