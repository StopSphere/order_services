package com.order_service.shopsphere.order_service.DTO.response;

import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrderResponseDTO {

    private UUID orderId;
    private UUID userId;
    private UUID productId;
    private Integer quantity;
    private OrderStatus status;
    private BigDecimal totalAmount;
}
