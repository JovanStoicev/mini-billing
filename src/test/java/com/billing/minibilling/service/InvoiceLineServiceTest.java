package com.billing.minibilling.service;

import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceLineServiceTest {
    private final InvoiceLineService invoiceLineService = new InvoiceLineService();

    @Test
    void createsSingleLineWhenOnePriceCoversMeasurementPeriod() {
        Measurement measurement = new Measurement(
                "1",
                "gas",
                OffsetDateTime.parse("2024-03-01T00:00:00+02:00"),
                OffsetDateTime.parse("2024-03-31T23:59:59+02:00"),
                new BigDecimal("87")
        );
        List<Price> prices = List.of(new Price(
                "gas",
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-12-31"),
                new BigDecimal("1.8")
        ));

        List<InvoiceLine> lines = invoiceLineService.createInvoiceLines(measurement, prices, 1);

        assertEquals(1, lines.size());
        assertEquals(1, lines.getFirst().getIndex());
        assertEquals(new BigDecimal("87.00"), lines.getFirst().getQuantity());
        assertEquals(new BigDecimal("1.8"), lines.getFirst().getPrice());
        assertEquals(new BigDecimal("156.60"), lines.getFirst().getAmount());
    }

    @Test
    void splitsLineWhenPriceChangesDuringMeasurementPeriod() {
        Measurement measurement = new Measurement(
                "1",
                "gas",
                OffsetDateTime.parse("2024-02-28T00:00:00+02:00"),
                OffsetDateTime.parse("2024-03-02T23:59:59+02:00"),
                new BigDecimal("10")
        );
        List<Price> prices = List.of(
                new Price(
                        "gas",
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-02-29"),
                        new BigDecimal("0.4")
                ),
                new Price(
                        "gas",
                        LocalDate.parse("2024-03-01"),
                        LocalDate.parse("2024-12-31"),
                        new BigDecimal("4.4")
                )
        );

        List<InvoiceLine> lines = invoiceLineService.createInvoiceLines(measurement, prices, 2);

        assertEquals(2, lines.size());
        assertEquals(new BigDecimal("5.00"), lines.get(0).getQuantity());
        assertEquals(new BigDecimal("0.4"), lines.get(0).getPrice());
        assertEquals(new BigDecimal("2.00"), lines.get(0).getAmount());
        assertEquals(new BigDecimal("5.00"), lines.get(1).getQuantity());
        assertEquals(new BigDecimal("4.4"), lines.get(1).getPrice());
        assertEquals(new BigDecimal("22.00"), lines.get(1).getAmount());
    }

    @Test
    void keepsTotalQuantityWhenSplittingIntoThreeEqualPeriods() {
        Measurement measurement = new Measurement(
                "1",
                "gas",
                OffsetDateTime.parse("2024-01-01T00:00:00+02:00"),
                OffsetDateTime.parse("2024-01-03T23:59:59+02:00"),
                new BigDecimal("100")
        );
        List<Price> prices = List.of(
                new Price(
                        "gas",
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-01-01"),
                        new BigDecimal("1.0")
                ),
                new Price(
                        "gas",
                        LocalDate.parse("2024-01-02"),
                        LocalDate.parse("2024-01-02"),
                        new BigDecimal("1.0")
                ),
                new Price(
                        "gas",
                        LocalDate.parse("2024-01-03"),
                        LocalDate.parse("2024-01-03"),
                        new BigDecimal("1.0")
                )
        );

        List<InvoiceLine> lines = invoiceLineService.createInvoiceLines(measurement, prices, 1);

        BigDecimal totalQuantity = lines.stream()
                .map(InvoiceLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("100.00"), totalQuantity);
    }

    @Test
    void distributesQuantityProportionallyByDays() {
        Measurement measurement = new Measurement(
                "1",
                "elec",
                OffsetDateTime.parse("2023-11-01T13:23:00+02:00"),
                OffsetDateTime.parse("2023-11-30T15:20:00+02:00"),
                new BigDecimal("120")
        );
        List<Price> prices = List.of(
                new Price(
                        "elec",
                        LocalDate.parse("2023-10-25"),
                        LocalDate.parse("2023-11-06"),
                        new BigDecimal("0.30")
                ),
                new Price(
                        "elec",
                        LocalDate.parse("2023-11-07"),
                        LocalDate.parse("2023-11-18"),
                        new BigDecimal("0.35")
                ),
                new Price(
                        "elec",
                        LocalDate.parse("2023-11-19"),
                        LocalDate.parse("2023-12-04"),
                        new BigDecimal("0.32")
                )
        );

        List<InvoiceLine> lines = invoiceLineService.createInvoiceLines(measurement, prices, 1);

        assertEquals(3, lines.size());
        assertEquals(OffsetDateTime.parse("2023-11-01T13:23:00+02:00"), lines.get(0).getLineStart());
        assertEquals(OffsetDateTime.parse("2023-11-06T23:59:59+02:00"), lines.get(0).getLineEnd());
        assertEquals(new BigDecimal("24.00"), lines.get(0).getQuantity());
        assertEquals(new BigDecimal("0.30"), lines.get(0).getPrice());
        assertEquals(new BigDecimal("48.00"), lines.get(1).getQuantity());
        assertEquals(new BigDecimal("0.35"), lines.get(1).getPrice());
        assertEquals(OffsetDateTime.parse("2023-11-19T00:00:00+02:00"), lines.get(2).getLineStart());
        assertEquals(OffsetDateTime.parse("2023-11-30T15:20:00+02:00"), lines.get(2).getLineEnd());
        assertEquals(new BigDecimal("48.00"), lines.get(2).getQuantity());
        assertEquals(new BigDecimal("0.32"), lines.get(2).getPrice());
    }

    @Test
    void failsWhenPricesDoNotCoverFullMeasurementPeriod() {
        Measurement measurement = new Measurement(
                "1",
                "gas",
                OffsetDateTime.parse("2024-03-01T00:00:00+02:00"),
                OffsetDateTime.parse("2024-03-31T23:59:59+02:00"),
                new BigDecimal("10")
        );
        List<Price> prices = List.of(new Price(
                "gas",
                LocalDate.parse("2024-03-10"),
                LocalDate.parse("2024-03-31"),
                new BigDecimal("1.8")
        ));

        assertThrows(IllegalArgumentException.class, () ->
                invoiceLineService.createInvoiceLines(measurement, prices, 1)
        );
    }
}
