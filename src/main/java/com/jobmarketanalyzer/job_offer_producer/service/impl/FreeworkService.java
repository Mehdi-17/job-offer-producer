package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmarketanalyzer.job_offer_producer.config.WebDriverConfig;
import com.jobmarketanalyzer.job_offer_producer.model.JobOffer;
import com.jobmarketanalyzer.job_offer_producer.DTO.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.model.enums.SourceOffer;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import com.jobmarketanalyzer.job_offer_producer.service.JobScraper;
import com.jobmarketanalyzer.job_offer_producer.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeworkService implements FetchService, JobScraper {

    @Value("${freework.search.url}")
    private String freeworkSearchUrl;

    @Value("${freework.job.elements}")
    private String freeworkJobElements;

    private final WebDriverConfig.WebDriverManager webDriverManager;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<JobOffersDTO> fetchData() {
        return CompletableFuture.supplyAsync(() -> {
            Set<JobOffer> jobOffers = scrapeJobOffer();

            if (jobOffers.isEmpty()) {
                return null;
            }

            try {
                return new JobOffersDTO(SourceOffer.FREEWORK.name(), JsonUtils.buildJson(jobOffers, objectMapper));
            } catch (JsonProcessingException e) {
                log.error("Error when writing JSON string for Freework job.", e);
                throw new CompletionException(e);
            }
        }, executorService);
    }

    @Override
    public Set<JobOffer> scrapeJobOffer() {
        Set<JobOffer> jobOfferSet = new HashSet<>();
        WebDriver driver = webDriverManager.createWebDriver();

        try {
            log.info("Start headless browser to scrape freework job offers");
            driver.get(freeworkSearchUrl);

            //todo implementation multipage scrape.

            List<WebElement> elements = driver.findElements(By.cssSelector(freeworkJobElements));
            log.info("Find {} elements on freework first page.", elements.size());

            elements.forEach(jobElement -> jobOfferSet.add(
                    JobOffer
                            .builder()
                            .title(jobElement.findElement(By.tagName("h2")).getText())
                            .description(jobElement.findElement(By.tagName("p")).getText())
                            .dailyRate(jobElement.findElement(By.xpath(".//span[contains(text(), '€⁄j')]")).getText())
                            .build())
            );

            return jobOfferSet;
        }catch (Exception e){
            log.error("There is an unexpected error when scraping Freework.", e);
            return jobOfferSet;
        }finally {
            log.info("Quit freework headless browser");
            driver.quit();
        }
    }
}
