package com.billing.minibilling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Price {
    private String product;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
}
