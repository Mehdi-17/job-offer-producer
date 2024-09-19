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
import java.util.concurrent.TimeUnit;


import static com.jobmarketanalyzer.job_offer_producer.utils.UrlUtils.urlIsValid;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndeedService implements FetchService, JobScraper {

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

    @Value("${indeed.job.company.element}")
    private String indeedJobCompanyElement;

    @Value("${timeout.scraping}")
    private int timeoutValue;

    private final WebDriverConfig.WebDriverManager webDriverManager;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<JobOffersDTO> fetchData() {
        return CompletableFuture.supplyAsync(()->{
            Set<JobOffer> jobOffers = scrapeJobOffer();

            if (jobOffers.isEmpty()) {
                return null;
            }

            try {
                return new JobOffersDTO(SourceOffer.INDEED.name(), JsonUtils.buildJson(jobOffers, objectMapper));
            } catch (JsonProcessingException e) {
                log.error("Error when writing JSON string for Indeed job.", e);
               throw new CompletionException(e);
            }
        }, executorService).orTimeout(timeoutValue, TimeUnit.SECONDS);
    }

    @Override
    public Set<JobOffer> scrapeJobOffer() {
        Set<JobOffer> jobOfferSet = new HashSet<>();
        WebDriver driver = webDriverManager.createWebDriver();
        int pageIndex = 1;

        try {
            log.info("Start headless browser to scrape indeed job offers");
            driver.get(indeedSearchUrl);

            while (scrapeCurrentPage(driver, pageIndex, jobOfferSet)) {
                pageIndex++;
            }

            log.info("Scrape {} offers on indeed", jobOfferSet.size());
            return jobOfferSet;
        } catch (Exception e) {
            log.error("There is an unexpected erro when scraping Indeed.", e);
            return jobOfferSet;
        } finally {
            log.info("Quit Indeed headless browser");
            driver.quit();
        }
    }

    private boolean scrapeCurrentPage(WebDriver driver, int pageIndex, Set<JobOffer> jobOfferSet){
        List<String> jobIdList = extractJobId(driver);
        log.info("Indeed scraping: there is {} on the page {}", jobIdList.size(), pageIndex);

        for (String jobId : jobIdList) {
            try {
                String newJobUrl = driver.getCurrentUrl().replaceAll("(vjk=)[^&]*", "$1" + jobId);
                ScraperUtils.goTo(driver, newJobUrl, jobId);
                jobOfferSet.add(buildJobOffer(driver, jobId));
            } catch (Exception e) {
                log.error("Error processing job offer: {}", e.getMessage(), e);
                log.info("Scrape {} offers on indeed", jobOfferSet.size());
                return false;
            }
        }

        return navigateToNextPageIfExist(driver, jobIdList);
    }

    private boolean navigateToNextPageIfExist(WebDriver driver, List<String> jobIdList){
        if (!jobIdList.isEmpty()) {
            String nextUrl = getUrlNextPage(driver);

            if (urlIsValid(nextUrl)) {
                try {
                    ScraperUtils.goTo(driver, nextUrl, "vjk=");
                    return true;
                } catch (Exception e) {
                    log.error("Error when trying to open the next page : {}.", e.getMessage(), e);
                    return false;
                }
            }
        }

        return false;
    }

    private List<String> extractJobId(WebDriver driver){
        try {
            return driver.findElements(By.cssSelector(indeedJobElementsName)).stream()
                    .map(el -> el.findElement(By.tagName("a")).getAttribute("id").split("_")[1])
                    .toList();
        } catch (Exception e) {
            log.error("Error trying to find job id list.", e);
            return List.of();
        }
    }

    private String getUrlNextPage(WebDriver driver) {
        WebElement paginationNav = driver.findElement(By.cssSelector("nav[role='navigation'][aria-label='pagination'] ul"));
        List<WebElement> paginationItems = paginationNav.findElements(By.tagName("li"));

        if (paginationItems.isEmpty()) {
            log.info("Indeed scrapping : this was the last page.");
            return null;
        }

        WebElement lastPaginationItem = paginationItems.get(paginationItems.size() - 1);
        if(!lastPaginationItem.getText().isEmpty()){
            log.info("Indeed scrapping : this was the last page.");
            return null;
        }

        log.info("Indeed scrapping: there is a next page.");
        WebElement anchorTag = lastPaginationItem.findElement(By.tagName("a"));

        return anchorTag.getAttribute("href");
    }

    private JobOffer buildJobOffer(WebDriver driver, String jobId) throws Exception {
        try {
            log.info("Scrapping job id {}.", jobId);
            return JobOffer.builder()
                    .title(driver.findElement(By.cssSelector(indeedJobTitleElement)).getText().split("-")[0].trim())
                    .description(driver.findElement(By.id(indeedJobDescElement)).getAttribute("innerHTML"))
                    .dailyRate(driver.findElement(By.id(indeedJobSalaryElement)).getText())
                    .company(driver.findElement(By.cssSelector(indeedJobCompanyElement)).getText())
                    .build();

        } catch (Exception e) {
            throw new Exception("Element not found in page.", e);
        }
    }

}
