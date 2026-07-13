package com.billing.minibilling.service;

import com.billing.minibilling.model.Reading;
import com.billing.minibilling.reader.ReadingCsvReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingService {
    private final ReadingCsvReader readingCsvReader;

    public List<Reading> loadReadings(Path inputDirectory) {
        return readingCsvReader.read(inputDirectory);
    }

    public List<Reading> findByReferenceNumber(Path inputDirectory, String referenceNumber) {
        return loadReadings(inputDirectory).stream()
                .filter(reading -> reading.getReferenceNumber().equals(referenceNumber))
                .sorted(Comparator.comparing(Reading::getDate))
                .toList();
    }
}