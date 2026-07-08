package com.billing.minibilling.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private OffsetDateTime documentDate;
    private String documentNumber;
    private String consumer;
    private String reference;
    private BigDecimal totalAmount;
    private List<InvoiceLine> lines = new ArrayList<>();
}
