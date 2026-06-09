package com.order_service.shopsphere.order_service.Event;

import com.order_service.shopsphere.order_service.DTO.Event.InventoryFailedEvent;
import com.order_service.shopsphere.order_service.DTO.Event.InventoryReservedEvent;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Kafka.InventoryEventConsumer;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Test
    void shouldConfirmOrderSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CREATED);

        InventoryReservedEvent event =
                new InventoryReservedEvent(orderId);

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.of(order));

        inventoryEventConsumer
                .consumeInventoryReservedEvent(event);

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        verify(orderRepository)
                .save(order);
    }

    @Test
    void shouldCancelOrderSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CREATED);

        InventoryFailedEvent event =
                new InventoryFailedEvent(
                        orderId,
                        "Insufficient stock"
                );

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.of(order));

        inventoryEventConsumer
                .consumeInventoryFailedEvent(event);

        assertEquals(
                OrderStatus.CANCELLED,
                order.getStatus()
        );

        verify(orderRepository)
                .save(order);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForReservedEvent() {

        UUID orderId = UUID.randomUUID();

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.empty());

        InventoryReservedEvent event =
                new InventoryReservedEvent(orderId);

        assertThrows(
                RuntimeException.class,
                () -> inventoryEventConsumer
                        .consumeInventoryReservedEvent(event)
        );
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForFailedEvent() {

        UUID orderId = UUID.randomUUID();

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.empty());

        InventoryFailedEvent event =
                new InventoryFailedEvent(
                        orderId,
                        "Inventory Error"
                );

        assertThrows(
                RuntimeException.class,
                () -> inventoryEventConsumer
                        .consumeInventoryFailedEvent(event)
        );
    }
}