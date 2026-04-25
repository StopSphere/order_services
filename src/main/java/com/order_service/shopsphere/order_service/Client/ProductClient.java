package com.order_service.shopsphere.order_service.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "product-service", url = "http://localhost:8081/v1/api/products")
public interface ProductClient {

    @PatchMapping("/{id}/reduce")
    void reduceStock(@PathVariable UUID id, @RequestBody StockRequest stockRequest);
}
