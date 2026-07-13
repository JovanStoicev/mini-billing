package com.billing.minibilling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingInput {
    @NotBlank
    private String product;

    @NotNull
    private OffsetDateTime date;

    @NotNull
    private BigDecimal value;
}