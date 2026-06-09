package com.order_service.shopsphere.order_service.DTO.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductResponse {
    private UUID productId;

    private BigDecimal price;
}
