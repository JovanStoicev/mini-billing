package com.billing.minibilling.service;

import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Reading;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.billing.minibilling.util.Constants.BILLING_ZONE;

@Service
public class MeasurementService {
    public List<Measurement> calculateMonthlyMeasurements(List<Reading> readings, YearMonth billingMonth) {
        return readings.stream()
                .collect(Collectors.groupingBy(reading -> new ReadingKey(
                        reading.getReferenceNumber(),
                        reading.getProduct()
                )))
                .entrySet()
                .stream()
                .map(entry -> calculateMonthlyMeasurement(entry.getValue(), billingMonth))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparing(Measurement::getReferenceNumber)
                        .thenComparing(Measurement::getProduct))
                .toList();
    }

    public Optional<Measurement> calculateMonthlyMeasurement(List<Reading> readings, YearMonth billingMonth) {
        OffsetDateTime periodStart = billingMonth.atDay(1)
                .atStartOfDay(BILLING_ZONE)
                .toOffsetDateTime();
        OffsetDateTime periodEnd = billingMonth.atEndOfMonth()
                .atTime(23, 59, 59)
                .atZone(BILLING_ZONE)
                .toOffsetDateTime();

        Optional<Reading> previousReading = findLastBefore(readings, periodStart);
        Optional<Reading> currentReading = findLastBetween(readings, periodStart, periodEnd);

        if (previousReading.isEmpty() || currentReading.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(createMeasurement(previousReading.get(), currentReading.get()));
    }

    private Optional<Reading> findLastBefore(List<Reading> readings, OffsetDateTime date) {
        return readings.stream()
                .filter(reading -> reading.getDate().isBefore(date))
                .max(Comparator.comparing(Reading::getDate));
    }

    private Optional<Reading> findLastBetween(List<Reading> readings, OffsetDateTime start, OffsetDateTime end) {
        return readings.stream()
                .filter(reading -> !reading.getDate().isBefore(start))
                .filter(reading -> !reading.getDate().isAfter(end))
                .max(Comparator.comparing(Reading::getDate));
    }

    private Measurement createMeasurement(Reading previousReading, Reading currentReading) {
        BigDecimal quantity = currentReading.getValue().subtract(previousReading.getValue());
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("Current reading value cannot be lower than previous reading value");
        }

        return new Measurement(
                currentReading.getReferenceNumber(),
                currentReading.getProduct(),
                previousReading.getDate(),
                currentReading.getDate(),
                quantity
        );
    }

    private record ReadingKey(String referenceNumber, String product) {
    }
}