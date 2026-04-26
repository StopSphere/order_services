package com.order_service.shopsphere.order_service.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequestDTO {
    @NotNull
    private UUID productId;

    private UUID userId;

    @Min(1)
    private int quantity;


}
