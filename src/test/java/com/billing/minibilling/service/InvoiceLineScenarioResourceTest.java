package com.billing.minibilling.service;

import com.billing.minibilling.model.InvoiceLine;
import com.billing.minibilling.model.Measurement;
import com.billing.minibilling.model.Price;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class InvoiceLineScenarioResourceTest {
    private static final String PRODUCT = "elec";
    private static final String REFERENCE_NUMBER = "1";
    private static final int PRICE_LIST_NUMBER = 1;
    private static final Path TEST_SCENARIOS_ROOT = Path.of("src", "test", "resources");
    private static final Path MAIN_SCENARIOS_ROOT = Path.of("src", "main", "resources");

    private final InvoiceLineService invoiceLineService = new InvoiceLineService();

    @TestFactory
    Stream<DynamicTest> createsExpectedLinesFromScenarioResources() throws IOException {
        Path scenariosRoot = findScenariosRoot();
        if (scenariosRoot == null) {
            return Stream.of(skippedTest("No scenario resources are added yet"));
        }

        List<Path> scenarioDirectories;
        try (Stream<Path> paths = Files.list(scenariosRoot)) {
            scenarioDirectories = paths
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("sc"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        if (scenarioDirectories.isEmpty()) {
            return Stream.of(skippedTest("No sc1-sc5 scenario directories are added yet"));
        }

        return scenarioDirectories.stream()
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> assertScenario(path)));
    }

    private DynamicTest skippedTest(String reason) {
        return DynamicTest.dynamicTest(reason, () -> assumeTrue(false, reason));
    }

    private Path findScenariosRoot() throws IOException {
        if (containsScenarioDirectories(TEST_SCENARIOS_ROOT)) {
            return TEST_SCENARIOS_ROOT;
        }

        if (containsScenarioDirectories(MAIN_SCENARIOS_ROOT)) {
            return MAIN_SCENARIOS_ROOT;
        }

        return null;
    }

    private boolean containsScenarioDirectories(Path root) throws IOException {
        if (!Files.exists(root)) {
            return false;
        }

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .anyMatch(path -> path.getFileName().toString().startsWith("sc"));
        }
    }

    private void assertScenario(Path scenarioDirectory) throws IOException {
        ScenarioInput input = readInput(scenarioDirectory.resolve("in.txt"));
        List<ExpectedLine> expectedLines = readOutput(scenarioDirectory.resolve("out.txt"));

        List<InvoiceLine> actualLines = input.measurements().stream()
                .sorted(Comparator.comparing(Measurement::getStartDate))
                .map(measurement -> invoiceLineService.createInvoiceLines(
                        measurement,
                        input.prices(),
                        PRICE_LIST_NUMBER
                ))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(InvoiceLine::getLineStart))
                .toList();

        assertEquals(expectedLines.size(), actualLines.size(), "Different number of lines in " + scenarioDirectory);

        for (int index = 0; index < expectedLines.size(); index++) {
            ExpectedLine expectedLine = expectedLines.get(index);
            InvoiceLine actualLine = actualLines.get(index);

            assertEquals(expectedLine.start(), actualLine.getLineStart(), "lineStart at row " + index);
            assertEquals(expectedLine.end(), actualLine.getLineEnd(), "lineEnd at row " + index);
            assertSameNumber(expectedLine.quantity(), actualLine.getQuantity(), "quantity at row " + index);
            assertSameNumber(expectedLine.price(), actualLine.getPrice(), "price at row " + index);
        }
    }

    private ScenarioInput readInput(Path path) throws IOException {
        List<String> lines = readDataLines(path);
        int expectedRowCount = Integer.parseInt(stripBom(lines.getFirst()));
        List<String> dataRows = lines.subList(1, lines.size());

        assertEquals(expectedRowCount, dataRows.size(), "Invalid row count in " + path);

        List<Measurement> measurements = new ArrayList<>();
        List<Price> prices = new ArrayList<>();

        for (String row : dataRows) {
            String[] columns = row.split(",");
            if ("Q".equals(columns[0])) {
                measurements.add(new Measurement(
                        REFERENCE_NUMBER,
                        PRODUCT,
                        OffsetDateTime.parse(columns[1]),
                        OffsetDateTime.parse(columns[2]),
                        new BigDecimal(columns[3])
                ));
            } else if ("P".equals(columns[0])) {
                prices.add(new Price(
                        PRODUCT,
                        LocalDate.parse(columns[1]),
                        LocalDate.parse(columns[2]),
                        new BigDecimal(columns[3])
                ));
            } else {
                throw new IllegalArgumentException("Unsupported row type in " + path + ": " + row);
            }
        }

        assertTrue(!measurements.isEmpty(), "No Q rows in " + path);
        assertTrue(!prices.isEmpty(), "No P rows in " + path);

        return new ScenarioInput(measurements, prices);
    }

    private List<ExpectedLine> readOutput(Path path) throws IOException {
        return readDataLines(path).stream()
                .map(row -> row.split(","))
                .map(columns -> new ExpectedLine(
                        OffsetDateTime.parse(stripBom(columns[0])),
                        OffsetDateTime.parse(columns[1]),
                        new BigDecimal(columns[2]),
                        new BigDecimal(columns[3])
                ))
                .toList();
    }

    private List<String> readDataLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private void assertSameNumber(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), message);
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private record ScenarioInput(List<Measurement> measurements, List<Price> prices) {
    }

    private record ExpectedLine(
            OffsetDateTime start,
            OffsetDateTime end,
            BigDecimal quantity,
            BigDecimal price
    ) {
    }
}
