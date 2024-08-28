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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        try {
            return CompletableFuture.completedFuture(new JobOffersDTO(SourceOffer.INDEED.name(), buildJson(jobOffers)));
        } catch (JsonProcessingException e) {
            log.error("Error when writing JSON string for Indeed job.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

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

    //todo a chaque fois qu'on va selectionner un élément on le fait dans un try catch au lieu d'avoir tout le code dans un gros try catch
    //todo toujours une erreur à la page 5, voir s'il n'y a pas un blocage automatique
    private Set<JobOffer> scrapeJobOffer() {
        int pageIndex = 1;
        try {
            log.info("Start headless browser to scrape indeed job offers");
            driver.get(indeedSearchUrl);

            Set<JobOffer> jobOfferSet = new HashSet<>();
            boolean isNextPage = true;

            while (isNextPage) {
                List<String> jobIdList = driver.findElements(By.cssSelector(indeedJobElementsName)).stream().map(el -> el.findElement(By.tagName("a")).getAttribute("id").split("_")[1]).toList();
                log.info("Indeed scraping: there is {} on the page {}", jobIdList.size(), pageIndex);

                for (String jobId : jobIdList) {
                    String newUrl = driver.getCurrentUrl().replaceAll("(vjk=)[^&]*", "$1" + jobId);
                    driver.get(newUrl);

                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    wait.until(ExpectedConditions.urlContains(jobId));

                    jobOfferSet.add(buildJobOffer());
                }

                String nextUrl = getUrlNextPage();
                isNextPage = urlIsValid(nextUrl);
                if (isNextPage){
                    pageIndex++;
                    driver.get(nextUrl);
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    wait.until(ExpectedConditions.urlContains("vjk="));
                }
            }

            log.info("Scrape {} offers on indeed", jobOfferSet.size());
            return jobOfferSet;
        } catch (Exception e ){
            log.error("Error when scrapping Indeed on page {} : ", pageIndex, e);
        } finally {
            log.info("Quit headless browser");
            driver.quit();
        }

        return Set.of();
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

    private boolean urlIsValid(String url){
        if (url == null){
            return false;
        }
        String regExUrl = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        Matcher matcher = Pattern.compile(regExUrl).matcher(url);

        return matcher.matches();
    }


    private JobOffer buildJobOffer(){
        return JobOffer.builder()
                .title(driver.findElement(By.cssSelector(indeedJobTitleElement)).getText().split("-")[0].trim())
                .description(driver.findElement(By.id(indeedJobDescElement)).getAttribute("innerHTML"))
                .dailyRate(driver.findElement(By.id(indeedJobSalaryElement)).getText())
                .build();
    }

}
