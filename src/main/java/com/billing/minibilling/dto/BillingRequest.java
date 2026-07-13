package com.billing.minibilling.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingRequest {
    @NotBlank
    private String period;

    @NotBlank
    private String inputDirectory;

    @NotBlank
    private String outputDirectory;
}
