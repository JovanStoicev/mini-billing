package com.billing.minibilling.service;

import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import com.billing.minibilling.model.Reading;
import com.billing.minibilling.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BillingServiceTest {
    private final UserService userService = mock(UserService.class);
    private final ReadingService readingService = mock(ReadingService.class);
    private final PriceService priceService = mock(PriceService.class);
    private final MeasurementService measurementService = mock(MeasurementService.class);
    private final InvoiceLineService invoiceLineService = mock(InvoiceLineService.class);

    private final BillingService billingService = new BillingService(
            userService,
            readingService,
            priceService,
            measurementService,
            invoiceLineService
    );

    @Test
    void generatesInvoiceForUserWithMonthlyMeasurement() {
        Path inputDirectory = Path.of("input");
        YearMonth billingMonth = YearMonth.of(2024, 3);
        User user = new User("Marko", "1", 2);
        Reading reading = new Reading(
                "1",
                "gas",
                OffsetDateTime.parse("2024-03-17T13:20:00+03:00"),
                new BigDecimal("1567")
        );
        Measurement measurement = new Measurement(
                "1",
                "gas",
                OffsetDateTime.parse("2024-01-01T12:00:00+03:00"),
                OffsetDateTime.parse("2024-03-17T13:20:00+03:00"),
                new BigDecimal("87")
        );
        Price price = new Price(
                "gas",
                LocalDate.parse("2024-03-01"),
                LocalDate.parse("2024-12-31"),
                new BigDecimal("4.4")
        );
        InvoiceLine invoiceLine = new InvoiceLine(
                7,
                new BigDecimal("87.000"),
                measurement.getStartDate(),
                measurement.getEndDate(),
                "gas",
                new BigDecimal("4.4"),
                2,
                new BigDecimal("382.80")
        );

        when(userService.loadUsers(inputDirectory)).thenReturn(List.of(user));
        when(readingService.loadReadings(inputDirectory)).thenReturn(List.of(reading));
        when(priceService.loadPriceLists(inputDirectory)).thenReturn(Map.of(2, List.of(price)));
        when(measurementService.calculateMonthlyMeasurements(List.of(reading), billingMonth))
                .thenReturn(List.of(measurement));
        when(invoiceLineService.createInvoiceLines(measurement, List.of(price), 2))
                .thenReturn(List.of(invoiceLine));

        List<Invoice> invoices = billingService.generateInvoices(inputDirectory, billingMonth);

        assertEquals(1, invoices.size());
        Invoice invoice = invoices.getFirst();
        assertEquals("10000", invoice.getDocumentNumber());
        assertEquals("Marko", invoice.getConsumer());
        assertEquals("1", invoice.getReference());
        assertEquals(new BigDecimal("382.80"), invoice.getTotalAmount());
        assertEquals(1, invoice.getLines().size());
        assertEquals(1, invoice.getLines().getFirst().getIndex());
    }

    @Test
    void skipsUsersWithoutMeasurements() {
        Path inputDirectory = Path.of("input");
        YearMonth billingMonth = YearMonth.of(2024, 3);
        User user = new User("Marko", "1", 2);

        when(userService.loadUsers(inputDirectory)).thenReturn(List.of(user));
        when(readingService.loadReadings(inputDirectory)).thenReturn(List.of());
        when(priceService.loadPriceLists(inputDirectory)).thenReturn(Map.of());
        when(measurementService.calculateMonthlyMeasurements(List.of(), billingMonth)).thenReturn(List.of());

        List<Invoice> invoices = billingService.generateInvoices(inputDirectory, billingMonth);

        assertEquals(0, invoices.size());
        verifyNoInteractions(invoiceLineService);
    }

    @Test
    void failsWhenMeasurementHasNoUser() {
        Path inputDirectory = Path.of("input");
        YearMonth billingMonth = YearMonth.of(2024, 3);
        Measurement measurement = new Measurement(
                "missing-reference",
                "gas",
                OffsetDateTime.parse("2024-01-01T12:00:00+03:00"),
                OffsetDateTime.parse("2024-03-17T13:20:00+03:00"),
                new BigDecimal("87")
        );

        when(userService.loadUsers(inputDirectory)).thenReturn(List.of());
        when(readingService.loadReadings(inputDirectory)).thenReturn(List.of());
        when(priceService.loadPriceLists(inputDirectory)).thenReturn(Map.of());
        when(measurementService.calculateMonthlyMeasurements(List.of(), billingMonth))
                .thenReturn(List.of(measurement));

        assertThrows(IllegalArgumentException.class, () ->
                billingService.generateInvoices(inputDirectory, billingMonth)
        );
    }
}