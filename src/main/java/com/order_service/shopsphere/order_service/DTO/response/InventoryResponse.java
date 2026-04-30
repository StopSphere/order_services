package com.order_service.shopsphere.order_service.DTO.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InventoryResponse {

    private UUID productId;
    private Integer quantity;
}
