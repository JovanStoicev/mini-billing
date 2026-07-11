package com.billing.minibilling.service;

import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.model.InvoiceLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static com.billing.minibilling.util.Constants.BULGARIAN_MONTH_NAMES;
import static com.billing.minibilling.util.Constants.INVOICE_DATE_TIME_FORMATTER;

@Service
public class InvoiceWriterService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void writeInvoices(List<Invoice> invoices, Path outputDirectory, YearMonth billingMonth) {
        for (Invoice invoice : invoices) {
            writeInvoice(invoice, outputDirectory, billingMonth);
        }
    }

    private void writeInvoice(Invoice invoice, Path outputDirectory, YearMonth billingMonth) {
        Path consumerDirectory = outputDirectory.resolve(createConsumerDirectoryName(invoice));
        Path invoiceFile = consumerDirectory.resolve(createInvoiceFileName(invoice, billingMonth));

        try {
            Files.createDirectories(consumerDirectory);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(invoiceFile.toFile(), toJson(invoice));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write invoice to " + invoiceFile, exception);
        }
    }

    private String createConsumerDirectoryName(Invoice invoice) {
        return invoice.getConsumer() + "-" + invoice.getReference();
    }

    private String createInvoiceFileName(Invoice invoice, YearMonth billingMonth) {
        String monthName = BULGARIAN_MONTH_NAMES.get(billingMonth.getMonthValue() - 1);
        String year = String.format("%02d", billingMonth.getYear() % 100);

        return invoice.getDocumentNumber() + "-" + monthName + "-" + year + ".json";
    }

    private Map<String, Object> toJson(Invoice invoice) {
        return Map.of(
                "documentDate", formatDate(invoice.getDocumentDate()),
                "documentNumber", invoice.getDocumentNumber(),
                "consumer", invoice.getConsumer(),
                "reference", invoice.getReference(),
                "totalAmount", invoice.getTotalAmount(),
                "lines", invoice.getLines().stream()
                        .map(this::toJson)
                        .toList()
        );
    }

    private Map<String, Object> toJson(InvoiceLine line) {
        return Map.of(
                "index", line.getIndex(),
                "quantity", line.getQuantity(),
                "lineStart", formatDate(line.getLineStart()),
                "lineEnd", formatDate(line.getLineEnd()),
                "product", line.getProduct(),
                "price", line.getPrice(),
                "priceList", line.getPriceList(),
                "amount", line.getAmount()
        );
    }

    private String formatDate(OffsetDateTime date) {
        return INVOICE_DATE_TIME_FORMATTER.format(date.toInstant());
    }
}