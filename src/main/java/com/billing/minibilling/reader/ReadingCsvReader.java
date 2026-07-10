package com.billing.minibilling.reader;

import com.billing.minibilling.model.Reading;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static com.billing.minibilling.util.Constants.READINGS_FILE_NAME;
import static com.billing.minibilling.util.Constants.UTF_8_BOM;

@Component
public class ReadingCsvReader {
    public List<Reading> read(Path inputDirectory) {
        Path readingsFile = inputDirectory.resolve(READINGS_FILE_NAME);

        try {
            return Files.readAllLines(readingsFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(this::parseReading)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read readings from " + readingsFile, exception);
        }
    }

    private Reading parseReading(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 4) {
            throw new IllegalArgumentException("Invalid reading row: " + line);
        }

        return new Reading(
                stripBom(columns[0].trim()),
                columns[1].trim(),
                OffsetDateTime.parse(columns[2].trim()),
                new BigDecimal(columns[3].trim())
        );
    }

    private String stripBom(String value) {
        return value.startsWith(UTF_8_BOM) ? value.substring(1) : value;
    }
}
