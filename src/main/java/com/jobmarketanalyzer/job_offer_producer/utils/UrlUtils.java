package com.jobmarketanalyzer.job_offer_producer.utils;

import java.util.regex.Pattern;

public class UrlUtils {

    private static final String URL_REGEX = "^(https?|ftp)://[^\\s/$.?#].\\S*$";

    public static boolean urlIsValid(String url) {
        if (url == null) {
            return false;
        }

        return  Pattern.compile(URL_REGEX).matcher(url).matches();
    }
}
