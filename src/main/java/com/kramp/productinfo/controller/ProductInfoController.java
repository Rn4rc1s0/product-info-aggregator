package com.kramp.productinfo.controller;

import com.kramp.productinfo.application.ProductAggregationService;
import com.kramp.productinfo.domain.model.AggregatedProduct;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product-info")
@Validated
public class ProductInfoController {

    private final ProductAggregationService aggregationService;

    public ProductInfoController(ProductAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    /**
     * Example:
     * GET /product-info?productId=ABC123&market=pl-PL&customerId=789
     */
    @GetMapping
    public AggregatedProduct getProductInfo(
            @RequestParam @NotBlank String productId,
            @RequestParam @NotBlank String market,
            @RequestParam(required = false) String customerId
    ) {
        return aggregationService.aggregate(productId, market, customerId);
    }
}
