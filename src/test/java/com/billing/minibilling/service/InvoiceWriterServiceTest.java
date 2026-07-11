package com.billing.minibilling.service;

import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.model.InvoiceLine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceWriterServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InvoiceWriterService invoiceWriterService = new InvoiceWriterService();

    @TempDir
    Path outputDirectory;

    @Test
    void writesInvoiceJsonInConsumerDirectory() throws Exception {
        InvoiceLine invoiceLine = new InvoiceLine(
                1,
                new BigDecimal("87.000"),
                OffsetDateTime.parse("2024-03-01T00:00:00+02:00"),
                OffsetDateTime.parse("2024-03-17T13:20:00+03:00"),
                "gas",
                new BigDecimal("4.4"),
                2,
                new BigDecimal("382.80")
        );
        Invoice invoice = new Invoice(
                OffsetDateTime.parse("2024-03-31T12:30:45+03:00"),
                "10000",
                "Marko Boikov Tsvetkov",
                "1",
                new BigDecimal("382.80"),
                List.of(invoiceLine)
        );

        invoiceWriterService.writeInvoices(List.of(invoice), outputDirectory, YearMonth.of(2024, 3));

        Path invoiceFile = outputDirectory
                .resolve("Marko Boikov Tsvetkov-1")
                .resolve("10000-\u043c\u0430\u0440\u0442-24.json");

        assertTrue(Files.exists(invoiceFile));

        String content = Files.readString(invoiceFile, StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(content);

        assertEquals("2024-03-31T09:30:45Z", json.get("documentDate").asText());
        assertEquals("10000", json.get("documentNumber").asText());
        assertEquals("Marko Boikov Tsvetkov", json.get("consumer").asText());
        assertEquals("1", json.get("reference").asText());
        assertEquals(0, new BigDecimal("382.80").compareTo(json.get("totalAmount").decimalValue()));

        JsonNode line = json.get("lines").get(0);
        assertEquals(1, line.get("index").asInt());
        assertEquals(0, new BigDecimal("87.000").compareTo(line.get("quantity").decimalValue()));
        assertEquals("2024-02-29T22:00:00Z", line.get("lineStart").asText());
        assertEquals("2024-03-17T10:20:00Z", line.get("lineEnd").asText());
        assertEquals("gas", line.get("product").asText());
        assertEquals(0, new BigDecimal("4.4").compareTo(line.get("price").decimalValue()));
        assertEquals(2, line.get("priceList").asInt());
        assertEquals(0, new BigDecimal("382.80").compareTo(line.get("amount").decimalValue()));
    }
}
