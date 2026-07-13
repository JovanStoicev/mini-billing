package com.billing.minibilling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BillingResponse {
    private int invoiceCount;
    private List<String> documentNumbers;
    private String outputDirectory;
}
