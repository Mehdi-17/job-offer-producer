package com.jobmarketanalyzer.job_offer_producer.service.impl;

import com.jobmarketanalyzer.job_offer_producer.model.JobOffersDTO;
import com.jobmarketanalyzer.job_offer_producer.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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

    @Override
    @Async
    public CompletableFuture<JobOffersDTO> fetchData() {
        scrapeJobOffer();
        return null;
    }

    private void scrapeJobOffer(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
        options.addArguments("--headless");

        log.info("Start headless browser to scrape indeed job offers");
        WebDriver driver = new ChromeDriver(options);
        driver.get(indeedSearchUrl);

        List<WebElement> elements = driver.findElements(By.cssSelector(indeedJobElementsName));
        log.info("Scrape {} offers on indeed", elements.size());

        for(WebElement element : elements){
            //todo for each element, get data of job offers.
            System.out.println(element.getAccessibleName());
        }
    }
}
