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
import com.jobmarketanalyzer.job_offer_producer.utils.ScraperUtils;
import com.jobmarketanalyzer.job_offer_producer.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.StringTemplate.STR;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeworkService implements FetchService, JobScraper {

    @Value("${freework.search.url}")
    private String freeworkSearchUrl;

    @Value("${freework.job.elements}")
    private String freeworkJobElements;

    @Value("${freework.job.salary.element}")
    private String salaryElement;

    @Value("${freework.job.next.page.button}")
    private String nextButtonElement;

    @Value("${timeout.scraping}")
    private int timeoutValue;

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
        }, executorService).orTimeout(timeoutValue, TimeUnit.SECONDS);
    }

    @Override
    public Set<JobOffer> scrapeJobOffer() {
        Set<JobOffer> jobOfferSet = new HashSet<>();
        WebDriver driver = webDriverManager.createWebDriver();

        try {
            log.info("Start headless browser to scrape freework job offers");
            driver.get(freeworkSearchUrl);

            boolean isNextPage = true;
            int pageIndex = 1;

            while (isNextPage){
                List<WebElement> elements = driver.findElements(By.cssSelector(freeworkJobElements));
                log.info("Find {} elements on freework at page {}.", elements.size(), pageIndex);

                elements.forEach(jobElement -> jobOfferSet.add(
                        JobOffer
                                .builder()
                                .title(jobElement.findElement(By.tagName("h2")).getText())
                                .description(jobElement.findElement(By.tagName("p")).getText())
                                .dailyRate(jobElement.findElement(By.xpath(salaryElement)).getText())
                                .company(jobElement.findElement(By.className("font-bold")).getText())
                                .build())
                );

                String urlNextPage = STR."\{driver.getCurrentUrl()}&page=\{++pageIndex}";
                isNextPage = UrlUtils.urlIsValid(urlNextPage) && checkIfNextButtonExist(driver, pageIndex);

                if (isNextPage){
                    try {
                        String expectedInUrl = STR."&page=\{pageIndex}";
                        ScraperUtils.goTo(driver, urlNextPage, expectedInUrl);
                    }catch (Exception e){
                        log.error("Error when trying to open the next page : {}.", e.getMessage(), e);
                        log.info("Scrape {} offers on freework", jobOfferSet.size());
                        return jobOfferSet;
                    }
                }
            }

            return jobOfferSet;
        }catch (Exception e){
            log.error("There is an unexpected error when scraping Freework.", e);
            return jobOfferSet;
        }finally {
            log.info("Quit freework headless browser");
            driver.quit();
        }
    }

    private boolean checkIfNextButtonExist(WebDriver driver, int pageIndex){
        String cssSelector = STR."\{nextButtonElement}'\{pageIndex}']";
        try {
            WebElement button = driver.findElement(By.cssSelector(cssSelector));
            return button.getAttribute("disabled") == null;
        }catch (NoSuchElementException e){
            return false;
        }
    }
}
