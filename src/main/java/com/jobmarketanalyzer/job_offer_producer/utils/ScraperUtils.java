package com.jobmarketanalyzer.job_offer_producer.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@Slf4j
public class ScraperUtils {

    private static final int HUMAN_DELAY_MILLIS = 2000;
    private static final int WAIT_TIMEOUT_SECONDS = 5;


    public static void chillBroImHuman() throws InterruptedException{
        log.info("Slow down, i'm a real human.");
        Thread.sleep(HUMAN_DELAY_MILLIS);
        log.info("Lets go bro...");
    }

    public static void goTo(WebDriver driver, String url, String expectedCondition) throws Exception {
        try {
            driver.get(url);
            ScraperUtils.chillBroImHuman();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
            wait.until(ExpectedConditions.urlContains(expectedCondition));
        } catch (Exception e) {
            throw new Exception("Failed to navigate to the URL or meet the expected condition.", e);
        }
    }
}
