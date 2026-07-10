package com.billing.minibilling.reader;

import com.billing.minibilling.model.User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class UserCsvReader {
    private static final String USERS_FILE_NAME = "users.csv";

    public List<User> read(Path inputDirectory) {
        Path usersFile = inputDirectory.resolve(USERS_FILE_NAME);

        try {
            return Files.readAllLines(usersFile, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(this::parseUser)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read users from " + usersFile, exception);
        }
    }

    private User parseUser(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 3) {
            throw new IllegalArgumentException("Invalid user row: " + line);
        }

        return new User(
                stripBom(columns[0].trim()),
                columns[1].trim(),
                Integer.parseInt(columns[2].trim())
        );
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
