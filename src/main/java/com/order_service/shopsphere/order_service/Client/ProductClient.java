package com.order_service.shopsphere.order_service.Client;

import com.order_service.shopsphere.order_service.DTO.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/v1/api/products/{productId}")
    ProductResponse getProduct(@PathVariable UUID productId) ;
}
