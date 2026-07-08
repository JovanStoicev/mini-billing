package com.billing.minibilling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLine {
    private int index;
    private BigDecimal quantity;
    private OffsetDateTime lineStart;
    private OffsetDateTime lineEnd;
    private String product;
    private BigDecimal price;
    private int priceList;
    private BigDecimal amount;
}
