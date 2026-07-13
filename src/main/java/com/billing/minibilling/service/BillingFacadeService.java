package com.billing.minibilling.service;

import com.billing.minibilling.model.Invoice;
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

    private YearMonth parseBillingMonth(String period) {
        try {
            return YearMonth.parse(period, BILLING_PERIOD_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Billing period must be in yy-MM format", exception);
        }
    }
}
