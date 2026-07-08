package com.billing.minibilling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Measurement {
    private String referenceNumber;
    private String product;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private BigDecimal quantity;
}
