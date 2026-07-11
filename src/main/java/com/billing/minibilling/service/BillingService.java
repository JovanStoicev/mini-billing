package com.billing.minibilling.service;

import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import com.billing.minibilling.model.Reading;
import com.billing.minibilling.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.billing.minibilling.util.Constants.AMOUNT_SCALE;
import static com.billing.minibilling.util.Constants.FIRST_DOCUMENT_NUMBER;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final UserService userService;
    private final ReadingService readingService;
    private final PriceService priceService;
    private final MeasurementService measurementService;
    private final InvoiceLineService invoiceLineService;

    public List<Invoice> generateInvoices(Path inputDirectory, YearMonth billingMonth) {
        List<User> users = userService.loadUsers(inputDirectory);
        List<Reading> readings = readingService.loadReadings(inputDirectory);
        Map<Integer, List<Price>> priceLists = priceService.loadPriceLists(inputDirectory);
        List<Measurement> measurements = measurementService.calculateMonthlyMeasurements(readings, billingMonth);

        Map<String, User> usersByReferenceNumber = users.stream()
                .collect(Collectors.toMap(User::getReferenceNumber, Function.identity()));
        validateUsersExistForMeasurements(measurements, usersByReferenceNumber);

        Map<String, List<Measurement>> measurementsByReferenceNumber = measurements.stream()
                .collect(Collectors.groupingBy(Measurement::getReferenceNumber));

        List<Invoice> invoices = new ArrayList<>();
        int documentNumber = FIRST_DOCUMENT_NUMBER;

        for (User user : users) {
            List<Measurement> userMeasurements = measurementsByReferenceNumber.getOrDefault(
                    user.getReferenceNumber(),
                    List.of()
            );

            if (userMeasurements.isEmpty()) {
                continue;
            }

            invoices.add(createInvoice(user, userMeasurements, priceLists, documentNumber));
            documentNumber++;
        }

        return invoices;
    }

    private Invoice createInvoice(
            User user,
            List<Measurement> measurements,
            Map<Integer, List<Price>> priceLists,
            int documentNumber
    ) {
        List<InvoiceLine> lines = measurements.stream()
                .sorted(Comparator.comparing(Measurement::getProduct))
                .map(measurement -> createInvoiceLines(user, measurement, priceLists))
                .flatMap(List::stream)
                .toList();

        reindexLines(lines);

        BigDecimal totalAmount = lines.stream()
                .map(InvoiceLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(AMOUNT_SCALE, RoundingMode.CEILING);

        return new Invoice(
                OffsetDateTime.now(ZoneOffset.UTC),
                String.valueOf(documentNumber),
                user.getName(),
                user.getReferenceNumber(),
                totalAmount,
                lines
        );
    }

    private List<InvoiceLine> createInvoiceLines(
            User user,
            Measurement measurement,
            Map<Integer, List<Price>> priceLists
    ) {
        List<Price> prices = priceLists.get(user.getPriceListNumber());
        if (prices == null) {
            throw new IllegalArgumentException("Missing price list: " + user.getPriceListNumber());
        }

        List<Price> productPrices = prices.stream()
                .filter(price -> price.getProduct().equals(measurement.getProduct()))
                .sorted(Comparator.comparing(Price::getStartDate))
                .toList();

        return invoiceLineService.createInvoiceLines(
                measurement,
                productPrices,
                user.getPriceListNumber()
        );
    }

    private void reindexLines(List<InvoiceLine> lines) {
        for (int index = 0; index < lines.size(); index++) {
            lines.get(index).setIndex(index + 1);
        }
    }

    private void validateUsersExistForMeasurements(
            List<Measurement> measurements,
            Map<String, User> usersByReferenceNumber
    ) {
        measurements.stream()
                .map(Measurement::getReferenceNumber)
                .filter(referenceNumber -> !usersByReferenceNumber.containsKey(referenceNumber))
                .findFirst()
                .ifPresent(referenceNumber -> {
                    throw new IllegalArgumentException("Missing user for reference number " + referenceNumber);
                });
    }
}