package com.order_service.shopsphere.order_service.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "product-service" )
public interface ProductClient {

    @PostMapping("/v1/api/products/{id}/reduce" )
    void reduceStock(@PathVariable UUID id, @RequestBody StockRequest stockRequest);
}
