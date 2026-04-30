package com.order_service.shopsphere.order_service.Client;

import com.order_service.shopsphere.order_service.DTO.response.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/v1/api/inventory/{productId}")
    InventoryResponse getStockByProductId(@PathVariable UUID productId) ;

    @PutMapping("/v1/api/inventory/remove")
    InventoryResponse removeStock(
            @RequestParam UUID productId,
            @RequestParam Integer quantity
    );
}
