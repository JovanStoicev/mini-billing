package com.billing.minibilling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reading {
    private String referenceNumber;
    private String product;
    private OffsetDateTime date;
    private BigDecimal value;
}
