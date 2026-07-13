package com.billing.minibilling;

import com.billing.minibilling.model.Invoice;
import com.billing.minibilling.service.BillingFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingCommandLineRunner implements CommandLineRunner {
    private static final int EXPECTED_ARGUMENTS_COUNT = 3;

    private final BillingFacadeService billingFacadeService;

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            return;
        }

        if (args.length != EXPECTED_ARGUMENTS_COUNT) {
            throw new IllegalArgumentException("Usage: java MiniBilling2025 <yy-MM> <input-directory> <output-directory>");
        }

        List<Invoice> invoices = billingFacadeService.generateAndWriteInvoices(args[0], args[1], args[2]);
        System.out.println("Generated invoices: " + invoices.size());
    }
}
