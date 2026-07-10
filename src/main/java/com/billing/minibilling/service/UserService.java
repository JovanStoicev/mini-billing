package com.billing.minibilling.service;

import com.billing.minibilling.model.User;
import com.billing.minibilling.reader.UserCsvReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserCsvReader userCsvReader;

    public List<User> loadUsers(Path inputDirectory) {
        return userCsvReader.read(inputDirectory);
    }

    public Optional<User> findByReferenceNumber(Path inputDirectory, String referenceNumber) {
        return loadUsers(inputDirectory).stream()
                .filter(user -> user.getReferenceNumber().equals(referenceNumber))
                .findFirst();
    }
}
