package com.billing.minibilling.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveBillingRequest {
    @NotBlank
    private String period;

    @NotBlank
    private String inputDirectory;

    @Valid
    @NotNull
    private List<ReadingInput> newReadings = new ArrayList<>();
}