package com.billing.minibilling.service;

import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.billing.minibilling.util.Constants.*;

@Service
public class InvoiceLineService {
    public List<InvoiceLine> createInvoiceLines(
            Measurement measurement,
            List<Price> prices,
            int priceListNumber
    ) {
        long totalSeconds = inclusiveSecondsBetween(measurement.getStartDate(), measurement.getEndDate());
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("Measurement end date must be after start date");
        }

        AtomicInteger lineIndex = new AtomicInteger(1);

        List<InvoiceLine> invoiceLines = prices.stream()
                .filter(price -> price.getProduct().equals(measurement.getProduct()))
                .sorted(Comparator.comparing(Price::getStartDate))
                .map(price -> createInvoiceLine(measurement, price, priceListNumber, totalSeconds, lineIndex))
                .flatMap(List::stream)
                .toList();

        if (invoiceLines.isEmpty()) {
            throw new IllegalArgumentException("Missing price for product " + measurement.getProduct());
        }

        validateFullPriceCoverage(invoiceLines, totalSeconds, measurement.getProduct());

        return invoiceLines;
    }

    private List<InvoiceLine> createInvoiceLine(
            Measurement measurement,
            Price price,
            int priceListNumber,
            long totalSeconds,
            AtomicInteger lineIndex
    ) {
        OffsetDateTime priceStart = price.getStartDate()
                .atStartOfDay(BILLING_ZONE)
                .toOffsetDateTime();
        OffsetDateTime priceEnd = price.getEndDate()
                .atTime(23, 59, 59)
                .atZone(BILLING_ZONE)
                .toOffsetDateTime();

        OffsetDateTime lineStart = max(measurement.getStartDate(), priceStart);
        OffsetDateTime lineEnd = min(measurement.getEndDate(), priceEnd);

        if (lineStart.isAfter(lineEnd)) {
            return List.of();
        }

        long lineSeconds = inclusiveSecondsBetween(lineStart, lineEnd);
        BigDecimal quantity = calculateProportionalQuantity(measurement.getQuantity(), lineSeconds, totalSeconds);
        BigDecimal amount = quantity.multiply(price.getAmount()).setScale(AMOUNT_SCALE, RoundingMode.CEILING);

        return List.of(new InvoiceLine(
                lineIndex.getAndIncrement(),
                quantity,
                lineStart,
                lineEnd,
                measurement.getProduct(),
                price.getAmount(),
                priceListNumber,
                amount
        ));
    }

    private void validateFullPriceCoverage(List<InvoiceLine> invoiceLines, long totalSeconds, String product) {
        long coveredSeconds = invoiceLines.stream()
                .mapToLong(line -> inclusiveSecondsBetween(line.getLineStart(), line.getLineEnd()))
                .sum();

        if (coveredSeconds != totalSeconds) {
            throw new IllegalArgumentException("Price periods do not cover the full measurement period for product " + product);
        }
    }

    private BigDecimal calculateProportionalQuantity(BigDecimal totalQuantity, long lineSeconds, long totalSeconds) {
        return totalQuantity
                .multiply(BigDecimal.valueOf(lineSeconds))
                .divide(BigDecimal.valueOf(totalSeconds), QUANTITY_SCALE, RoundingMode.CEILING);
    }

    private long inclusiveSecondsBetween(OffsetDateTime start, OffsetDateTime end) {
        return ChronoUnit.SECONDS.between(start, end) + 1;
    }

    private OffsetDateTime max(OffsetDateTime first, OffsetDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private OffsetDateTime min(OffsetDateTime first, OffsetDateTime second) {
        return first.isBefore(second) ? first : second;
    }
}