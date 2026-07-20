package com.billing.minibilling.util;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class Constants {
    public static final String USERS_FILE_NAME = "users.csv";
    public static final String READINGS_FILE_NAME = "readings.csv";
    public static final Pattern PRICE_FILE_PATTERN = Pattern.compile("prices-(\\d+)\\.csv");

    public static final String UTF_8_BOM = "\uFEFF";

    public static final ZoneId BILLING_ZONE = ZoneId.of("Europe/Sofia");
    public static final DateTimeFormatter INVOICE_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);
    public static final List<String> BULGARIAN_MONTH_NAMES = List.of(
            "януари",
            "февруари",
            "март",
            "април",
            "май",
            "юни",
            "юли",
            "август",
            "септември",
            "октомври",
            "ноември",
            "декември"
    );

    public static final int AMOUNT_SCALE = 2;
    public static final int FIRST_DOCUMENT_NUMBER = 10000;
}
