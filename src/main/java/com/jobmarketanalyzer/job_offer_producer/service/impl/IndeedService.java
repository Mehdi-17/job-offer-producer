package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndeedService implements FetchService {

    @Value("${indeed.search.url}")
    private String indeedSearchUrl;

    @Value("${indeed.job.elements}")
    private String indeedJobElementsName;

    @Value("${indeed.job.title.element}")
    private String indeedJobTitleElement;

    @Value("${indeed.job.desc.element}")
    private String indeedJobDescElement;

    @Value("${indeed.job.salary.element}")
    private String indeedJobSalaryElement;

    private final WebDriver driver;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchData() {

        Set<JobOffer> jobOffers = scrapeJobOffer();

        if (jobOffers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JobOffer jobOffer : jobOffers) {
            ObjectNode jobNode = objectMapper.createObjectNode();
            jobNode.put("title", jobOffer.title());
            jobNode.put("description", jobOffer.description());
            jobNode.put("dailyRate", jobOffer.dailyRate());
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

    private Set<JobOffer> scrapeJobOffer() {
        try {
            log.info("Start headless browser to scrape indeed job offers");
            driver.get(indeedSearchUrl);

            List<String> jobIdList = driver.findElements(By.cssSelector(indeedJobElementsName)).stream().map(el->el.findElement(By.tagName("a")).getAttribute("id").split("_")[1]).toList();
            log.info("Scrape {} offers on indeed", jobIdList.size());

            Set<JobOffer> jobOfferSet = new HashSet<>();

            for (String jobId : jobIdList) {
                String newUrl = driver.getCurrentUrl().replaceAll("(vjk=)[^&]*", "$1" + jobId);
                driver.get(newUrl);

                JobOffer scrappedOffer = JobOffer.builder()
                        .title(driver.findElement(By.cssSelector(indeedJobTitleElement)).getText().split("-")[0].trim())
                        .description(driver.findElement(By.id(indeedJobDescElement)).getAttribute("innerHTML"))
                        .dailyRate(driver.findElement(By.id(indeedJobSalaryElement)).getText())
                        .build();

                jobOfferSet.add(scrappedOffer);
            }

            //TODO: prendre en compte quand on a plusieurs pages de résultats.
            return jobOfferSet;
        } finally {
            log.info("Quit headless browser");
            driver.quit();
        }
    }
}
