package com.billing.minibilling.service;

import com.billing.minibilling.model.Price;
import com.billing.minibilling.reader.PriceCsvReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceCsvReader priceCsvReader;

    public Map<Integer, List<Price>> loadPriceLists(Path inputDirectory) {
        return priceCsvReader.readAll(inputDirectory);
    }

    public List<Price> loadPriceList(Path inputDirectory, int priceListNumber) {
        List<Price> prices = loadPriceLists(inputDirectory).get(priceListNumber);
        if (prices == null) {
            throw new IllegalArgumentException("Missing price list: " + priceListNumber);
        }

        return prices;
    }

    public List<String> findProductsByPriceListNumber(Path inputDirectory, int priceListNumber) {
        return loadPriceList(inputDirectory, priceListNumber).stream()
                .map(Price::getProduct)
                .distinct()
                .sorted()
                .toList();
    }
}
