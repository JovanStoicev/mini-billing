package com.billing.minibilling.service;

import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Reading;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementServiceTest {
    private final MeasurementService measurementService = new MeasurementService();

    @Test
    void createsMeasurementForProductWithPreviousReadingBeforeBillingMonth() {
        List<Reading> readings = List.of(
                new Reading("1", "gas", OffsetDateTime.parse("2024-01-01T12:00:00+03:00"), new BigDecimal("1480")),
                new Reading("1", "gas", OffsetDateTime.parse("2024-03-17T13:20:00+03:00"), new BigDecimal("1567"))
        );

        List<Measurement> measurements = measurementService.calculateMonthlyMeasurements(readings, YearMonth.of(2024, 3));

        assertEquals(1, measurements.size());
        assertEquals("gas", measurements.getFirst().getProduct());
        assertEquals(new BigDecimal("87"), measurements.getFirst().getQuantity());
    }

    @Test
    void createsMeasurementForNewProductWithTwoReadingsInsideBillingMonth() {
        List<Reading> readings = List.of(
                new Reading("1", "elec", OffsetDateTime.parse("2024-03-10T09:00:00+03:00"), new BigDecimal("500")),
                new Reading("1", "elec", OffsetDateTime.parse("2024-03-20T09:00:00+03:00"), new BigDecimal("575"))
        );

        List<Measurement> measurements = measurementService.calculateMonthlyMeasurements(readings, YearMonth.of(2024, 3));

        assertEquals(1, measurements.size());
        Measurement measurement = measurements.getFirst();
        assertEquals("elec", measurement.getProduct());
        assertEquals(OffsetDateTime.parse("2024-03-10T09:00:00+03:00"), measurement.getStartDate());
        assertEquals(OffsetDateTime.parse("2024-03-20T09:00:00+03:00"), measurement.getEndDate());
        assertEquals(new BigDecimal("75"), measurement.getQuantity());
    }

    @Test
    void skipsNewProductWithOnlyOneReadingInsideBillingMonth() {
        List<Reading> readings = List.of(
                new Reading("1", "elec", OffsetDateTime.parse("2024-03-10T09:00:00+03:00"), new BigDecimal("500"))
        );

        List<Measurement> measurements = measurementService.calculateMonthlyMeasurements(readings, YearMonth.of(2024, 3));

        assertEquals(0, measurements.size());
    }
}
