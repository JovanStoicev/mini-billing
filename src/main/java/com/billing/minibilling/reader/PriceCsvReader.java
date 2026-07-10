package com.billing.minibilling.reader;

import com.billing.minibilling.model.Price;
import com.billing.minibilling.util.Constants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.billing.minibilling.util.Constants.PRICE_FILE_PATTERN;

@Component
public class PriceCsvReader {
    public Map<Integer, List<Price>> readAll(Path inputDirectory) {
        try (Stream<Path> files = Files.list(inputDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isPriceFile)
                    .collect(Collectors.toMap(this::extractPriceListNumber, this::readPriceFile));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read price lists from " + inputDirectory, exception);
        }
    }

    private List<Price> readPriceFile(Path priceFile) {
        try {
            return Files.readAllLines(priceFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(this::parsePrice)
                    .sorted(Comparator.comparing(Price::getStartDate))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read prices from " + priceFile, exception);
        }
    }

    private boolean isPriceFile(Path file) {
        return PRICE_FILE_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private int extractPriceListNumber(Path file) {
        Matcher matcher = PRICE_FILE_PATTERN.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid price file name: " + file.getFileName());
        }

        return Integer.parseInt(matcher.group(1));
    }

    private Price parsePrice(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 4) {
            throw new IllegalArgumentException("Invalid price row: " + line);
        }

        return new Price(
                stripBom(columns[0].trim()),
                LocalDate.parse(columns[1].trim()),
                LocalDate.parse(columns[2].trim()),
                new BigDecimal(columns[3].trim())
        );
    }

    private String stripBom(String value) {
        return value.startsWith(Constants.UTF_8_BOM) ? value.substring(1) : value;
    }
}
