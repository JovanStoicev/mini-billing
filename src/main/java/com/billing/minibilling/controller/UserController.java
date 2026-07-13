package com.billing.minibilling.controller;

import com.billing.minibilling.model.Reading;
import com.billing.minibilling.model.User;
import com.billing.minibilling.service.ReadingService;
import com.billing.minibilling.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ReadingService readingService;

    @GetMapping("/users")
    public List<User> getUsers(@RequestParam String inputDirectory) {
        return userService.loadUsers(Path.of(inputDirectory));
    }

    @GetMapping("/users/{referenceNumber}")
    public User getUser(
            @PathVariable String referenceNumber,
            @RequestParam String inputDirectory
    ) {
        return userService.findByReferenceNumber(Path.of(inputDirectory), referenceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Missing user for reference number " + referenceNumber));
    }

    @GetMapping("/users/{referenceNumber}/readings")
    public List<Reading> getReadings(
            @PathVariable String referenceNumber,
            @RequestParam String inputDirectory
    ) {
        return readingService.findByReferenceNumber(Path.of(inputDirectory), referenceNumber);
    }
}
