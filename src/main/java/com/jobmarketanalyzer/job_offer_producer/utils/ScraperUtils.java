package com.jobmarketanalyzer.job_offer_producer.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScraperUtils {

    private static final int HUMAN_DELAY_MILLIS = 2000;

    public static void chillBroImHuman() throws InterruptedException{
        log.info("Slow down, i'm a real human.");
        Thread.sleep(HUMAN_DELAY_MILLIS);
        log.info("Lets go bro...");
    }
}
