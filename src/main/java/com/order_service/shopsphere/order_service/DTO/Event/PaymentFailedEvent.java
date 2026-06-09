package com.order_service.shopsphere.order_service.DTO.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {

    private UUID orderId;
    private String reason;
    private BigDecimal amount;
}
