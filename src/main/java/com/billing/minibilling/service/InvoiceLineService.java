package com.billing.minibilling.service;

import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

        List<LinePeriod> linePeriods = prices.stream()
                .filter(price -> price.getProduct().equals(measurement.getProduct()))
                .sorted(Comparator.comparing(Price::getStartDate))
                .map(price -> createLinePeriod(measurement, price))
                .flatMap(Optional::stream)
                .toList();

        if (linePeriods.isEmpty()) {
            throw new IllegalArgumentException("Missing price for product " + measurement.getProduct());
        }

        validateFullPriceCoverage(linePeriods, totalSeconds, measurement.getProduct());

        return createInvoiceLines(measurement, linePeriods, priceListNumber, totalSeconds);
    }

    private Optional<LinePeriod> createLinePeriod(Measurement measurement, Price price) {
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
            return Optional.empty();
        }

        return Optional.of(new LinePeriod(lineStart, lineEnd, price));
    }

    private List<InvoiceLine> createInvoiceLines(
            Measurement measurement,
            List<LinePeriod> linePeriods,
            int priceListNumber,
            long totalSeconds
    ) {
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        BigDecimal distributedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.CEILING);
        BigDecimal totalQuantity = measurement.getQuantity().setScale(QUANTITY_SCALE, RoundingMode.CEILING);

        for (int index = 0; index < linePeriods.size(); index++) {
            LinePeriod linePeriod = linePeriods.get(index);
            boolean isLastLine = index == linePeriods.size() - 1;
            BigDecimal quantity = isLastLine
                    ? totalQuantity.subtract(distributedQuantity)
                    : calculateProportionalQuantity(totalQuantity, linePeriod.seconds(), totalSeconds);
            BigDecimal amount = quantity.multiply(linePeriod.price().getAmount()).setScale(AMOUNT_SCALE, RoundingMode.CEILING);

            invoiceLines.add(new InvoiceLine(
                    index + 1,
                    quantity,
                    linePeriod.start(),
                    linePeriod.end(),
                    measurement.getProduct(),
                    linePeriod.price().getAmount(),
                    priceListNumber,
                    amount
            ));

            distributedQuantity = distributedQuantity.add(quantity);
        }

        return invoiceLines;
    }

    private void validateFullPriceCoverage(List<LinePeriod> linePeriods, long totalSeconds, String product) {
        long coveredSeconds = linePeriods.stream()
                .mapToLong(LinePeriod::seconds)
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

    private record LinePeriod(OffsetDateTime start, OffsetDateTime end, Price price) {
        private long seconds() {
            return ChronoUnit.SECONDS.between(start, end) + 1;
        }
    }
}