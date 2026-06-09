package com.order_service.shopsphere.order_service.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="orders")
@Getter  @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false ,columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private OrderStatus status;

    private BigDecimal totalAmount;

}
