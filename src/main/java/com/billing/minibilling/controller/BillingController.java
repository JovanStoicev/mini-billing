package com.billing.minibilling.controller;

import com.billing.minibilling.dto.BillingRequest;
import com.billing.minibilling.dto.BillingResponse;
import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.service.BillingFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BillingController {
    private final BillingFacadeService billingFacadeService;

    @PostMapping("/billing")
    public BillingResponse generateInvoices(@Valid @RequestBody BillingRequest request) {
        List<Invoice> invoices = billingFacadeService.generateAndWriteInvoices(
                request.getPeriod(),
                request.getInputDirectory(),
                request.getOutputDirectory()
        );

        return new BillingResponse(
                invoices.size(),
                invoices.stream()
                        .map(Invoice::getDocumentNumber)
                        .toList(),
                request.getOutputDirectory()
        );
    }
}
