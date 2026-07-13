package com.billing.minibilling.service;

import com.billing.minibilling.dto.ReadingInput;
import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.model.Reading;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingFacadeService {
    private static final DateTimeFormatter BILLING_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yy-MM");

    private final BillingService billingService;
    private final InvoiceWriterService invoiceWriterService;

    public List<Invoice> generateAndWriteInvoices(String period, String inputDirectory, String outputDirectory) {
        YearMonth billingMonth = parseBillingMonth(period);
        Path inputPath = Path.of(inputDirectory);
        Path outputPath = Path.of(outputDirectory);

        List<Invoice> invoices = billingService.generateInvoices(inputPath, billingMonth);
        invoiceWriterService.writeInvoices(invoices, outputPath, billingMonth);

        return invoices;
    }

    public Invoice previewInvoice(
            String period,
            String inputDirectory,
            String referenceNumber,
            List<ReadingInput> newReadings
    ) {
        YearMonth billingMonth = parseBillingMonth(period);
        List<Reading> readings = newReadings.stream()
                .map(reading -> new Reading(
                        referenceNumber,
                        reading.getProduct(),
                        reading.getDate(),
                        reading.getValue()
                ))
                .toList();

        return billingService.previewInvoice(Path.of(inputDirectory), billingMonth, referenceNumber, readings);
    }

    private YearMonth parseBillingMonth(String period) {
        try {
            return YearMonth.parse(period, BILLING_PERIOD_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Billing period must be in yy-MM format", exception);
        }
    }
}