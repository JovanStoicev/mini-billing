package com.billing.minibilling.service;

import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.billing.minibilling.util.Constants.*;

@Service
public class InvoiceLineService {
    public List<InvoiceLine> createInvoiceLines(
            Measurement measurement,
            List<Price> prices,
            int priceListNumber
    ) {
        long totalDays = inclusiveDaysBetween(measurement.getStartDate(), measurement.getEndDate());
        if (totalDays <= 0) {
            throw new IllegalArgumentException("Measurement end date must be after start date");
        }

        List<Price> productPrices = prices.stream()
                .filter(price -> price.getProduct().equals(measurement.getProduct()))
                .sorted(Comparator.comparing(Price::getStartDate))
                .toList();
        List<LinePeriod> linePeriods = createLinePeriods(measurement, productPrices);

        if (linePeriods.isEmpty()) {
            throw new IllegalArgumentException("Missing price for product " + measurement.getProduct());
        }

        validateFullPriceCoverage(linePeriods, totalDays, measurement.getProduct());

        return createInvoiceLines(measurement, linePeriods, priceListNumber, totalDays);
    }

    private List<LinePeriod> createLinePeriods(Measurement measurement, List<Price> prices) {
        List<LinePeriod> linePeriods = new ArrayList<>();
        LocalDate measurementStartDate = measurement.getStartDate().toLocalDate();
        LocalDate measurementEndDate = measurement.getEndDate().toLocalDate();
        OffsetDateTime nextStart = measurement.getStartDate();

        for (Price price : prices) {
            if (price.getEndDate().isBefore(measurementStartDate)) {
                continue;
            }

            if (price.getStartDate().isAfter(measurementEndDate)) {
                break;
            }

            OffsetDateTime lineStart = price.getStartDate().isAfter(nextStart.toLocalDate())
                    ? priceStart(price)
                    : nextStart;
            OffsetDateTime lineEnd = price.getEndDate().isBefore(measurementEndDate)
                    ? priceEnd(price)
                    : measurement.getEndDate();

            if (!lineStart.isAfter(lineEnd)) {
                linePeriods.add(new LinePeriod(lineStart, lineEnd, price));
                nextStart = lineEnd.plusSeconds(1);
            }
        }

        return linePeriods;
    }

    private OffsetDateTime priceStart(Price price) {
        return price.getStartDate()
                .atStartOfDay(BILLING_ZONE)
                .toOffsetDateTime();
    }

    private OffsetDateTime priceEnd(Price price) {
        return price.getEndDate()
                .atStartOfDay(BILLING_ZONE)
                .toOffsetDateTime()
                .withHour(23)
                .withMinute(59)
                .withSecond(59);
    }

    private List<InvoiceLine> createInvoiceLines(
            Measurement measurement,
            List<LinePeriod> linePeriods,
            int priceListNumber,
            long totalDays
    ) {
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        BigDecimal distributedQuantity = BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.CEILING);
        BigDecimal totalQuantity = measurement.getQuantity().setScale(AMOUNT_SCALE, RoundingMode.CEILING);

        for (int index = 0; index < linePeriods.size(); index++) {
            LinePeriod linePeriod = linePeriods.get(index);
            boolean isLastLine = index == linePeriods.size() - 1;
            BigDecimal quantity = isLastLine
                    ? totalQuantity.subtract(distributedQuantity)
                    : calculateProportionalQuantity(totalQuantity, linePeriod.days(), totalDays);
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

    private void validateFullPriceCoverage(List<LinePeriod> linePeriods, long totalDays, String product) {
        long coveredDays = linePeriods.stream()
                .mapToLong(LinePeriod::days)
                .sum();

        if (coveredDays != totalDays) {
            throw new IllegalArgumentException("Price periods do not cover the full measurement period for product " + product);
        }
    }

    private BigDecimal calculateProportionalQuantity(BigDecimal totalQuantity, long lineDays, long totalDays) {
        BigDecimal ratio = BigDecimal.valueOf(lineDays)
                .divide(BigDecimal.valueOf(totalDays), AMOUNT_SCALE, RoundingMode.CEILING);

        return totalQuantity.multiply(ratio).setScale(AMOUNT_SCALE, RoundingMode.CEILING);
    }

    private long inclusiveDaysBetween(OffsetDateTime start, OffsetDateTime end) {
        return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
    }

    private record LinePeriod(OffsetDateTime start, OffsetDateTime end, Price price) {
        private long days() {
            return inclusiveDaysBetween(start, end);
        }

        private static long inclusiveDaysBetween(OffsetDateTime start, OffsetDateTime end) {
            return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
        }
    }
}
