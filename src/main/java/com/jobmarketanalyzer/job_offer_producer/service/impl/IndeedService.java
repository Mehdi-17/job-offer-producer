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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


import static com.jobmarketanalyzer.job_offer_producer.utils.UrlUtils.urlIsValid;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndeedService implements FetchService {

    private static final int WAIT_TIMEOUT_SECONDS = 5;
    private static final int HUMAN_DELAY_MILLIS = 2000;

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

        try {
            return CompletableFuture.completedFuture(new JobOffersDTO(SourceOffer.INDEED.name(), buildJson(jobOffers)));
        } catch (JsonProcessingException e) {
            log.error("Error when writing JSON string for Indeed job.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    //todo To extract when the freework scraper will be in building
    private String buildJson(Set<JobOffer> jobOffers) throws JsonProcessingException {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (JobOffer jobOffer : jobOffers) {
            ObjectNode jobNode = objectMapper.createObjectNode();
            jobNode.put("title", jobOffer.title());
            jobNode.put("description", jobOffer.description());
            jobNode.put("dailyRate", jobOffer.dailyRate());
            arrayNode.add(jobNode);
        }

        return objectMapper.writeValueAsString(arrayNode);
    }

    private Set<JobOffer> scrapeJobOffer() {
        Set<JobOffer> jobOfferSet = new HashSet<>();

        try {
            log.info("Start headless browser to scrape indeed job offers");
            driver.get(indeedSearchUrl);

            int pageIndex = 1;
            boolean isNextPage = true;

            while (isNextPage) {
                List<String> jobIdList = extractJobId();
                log.info("Indeed scraping: there is {} on the page {}", jobIdList.size(), pageIndex);

                for (String jobId : jobIdList) {
                    String newJobUrl = driver.getCurrentUrl().replaceAll("(vjk=)[^&]*", "$1" + jobId);

                    try {
                        goTo(newJobUrl, jobId);
//                        slowDownImAHumanHahaDontWorryBro();
                        jobOfferSet.add(buildJobOffer(jobId));
                    } catch (Exception e) {
                        log.error("Error processing job offer: {}", e.getMessage(), e);
                        log.info("Scrape {} offers on indeed", jobOfferSet.size());
                        return jobOfferSet;
                    }
                }

                String nextUrl = getUrlNextPage();
                isNextPage = urlIsValid(nextUrl);
                if (isNextPage) {
                    pageIndex++;

                    try {
                        goTo(nextUrl, "vjk=");
                    } catch (Exception e) {
                        log.error("Error when trying to open the next page : {}.", e.getMessage(), e);
                        log.info("Scrape {} offers on indeed", jobOfferSet.size());
                        return jobOfferSet;
                    }
                }
            }

            log.info("Scrape {} offers on indeed", jobOfferSet.size());
            return jobOfferSet;
        } catch (Exception e) {
            log.error("There is an unexpected exception.", e);
            return jobOfferSet;
        } finally {
            log.info("Quit headless browser");
            driver.quit();
        }
    }

    private List<String> extractJobId(){
        try {
            return driver.findElements(By.cssSelector(indeedJobElementsName)).stream()
                    .map(el -> el.findElement(By.tagName("a")).getAttribute("id").split("_")[1])
                    .toList();
        } catch (Exception e) {
            log.error("Error trying to find job id list.", e);
            return List.of();
        }
    }

    private void goTo(String url, String expectedCondition) throws Exception {
        try {
            driver.get(url);
            slowDownImAHumanHahaDontWorryBro();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
            wait.until(ExpectedConditions.urlContains(expectedCondition));
        } catch (Exception e) {
            throw new Exception("Failed to navigate to the URL or meet the expected condition.", e);
        }
    }

    private String getUrlNextPage() {
        WebElement paginationNav = driver.findElement(By.cssSelector("nav[role='navigation'][aria-label='pagination'] ul"));
        List<WebElement> paginationItems = paginationNav.findElements(By.tagName("li"));

        if (paginationItems.isEmpty()) {
            log.info("Indeed scrapping : this was the last page.");
            return null;
        }

        log.info("Indeed scrapping: there is a next page.");
        WebElement lastPaginationItem = paginationItems.get(paginationItems.size() - 1);

        WebElement anchorTag = lastPaginationItem.findElement(By.tagName("a"));

        return anchorTag.getAttribute("href");
    }

    private JobOffer buildJobOffer(String jobId) throws Exception {
        try {
            log.info("Scrapping job id {}.", jobId);
            return JobOffer.builder()
                    .title(driver.findElement(By.cssSelector(indeedJobTitleElement)).getText().split("-")[0].trim())
                    .description(driver.findElement(By.id(indeedJobDescElement)).getAttribute("innerHTML"))
                    .dailyRate(driver.findElement(By.id(indeedJobSalaryElement)).getText())
                    .build();

        } catch (Exception e) {
            throw new Exception("Element not found in page.", e);
        }
    }

    private void slowDownImAHumanHahaDontWorryBro() throws InterruptedException {
        log.info("Slow down, i'm a real human.");
        Thread.sleep(HUMAN_DELAY_MILLIS);
        log.info("Lets go bro...");
    }

}
