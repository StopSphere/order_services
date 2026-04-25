package com.order_service.shopsphere.order_service.DTO.response;

import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrderResponseDTO {
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private OrderStatus status;

}
