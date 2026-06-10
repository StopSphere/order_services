package com.order_service.shopsphere.order_service.Event;

import com.order_service.shopsphere.order_service.DTO.Event.InventoryFailedEvent;
import com.order_service.shopsphere.order_service.DTO.Event.InventoryReservedEvent;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Kafka.InventoryEventConsumer;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    void shouldConfirmOrderSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CREATED);

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        orderId,
                        UUID.randomUUID(),
                        2,
                        BigDecimal.valueOf(1000)
                );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        inventoryEventConsumer
                .consumeInventoryReservedEvent(event);

        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, times(1))
                .save(orderCaptor.capture());

        Order savedOrder =
                orderCaptor.getValue();

        assertEquals(
                orderId,
                savedOrder.getOrderId()
        );

        assertEquals(
                OrderStatus.CONFIRMED,
                savedOrder.getStatus()
        );
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

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        inventoryEventConsumer
                .consumeInventoryFailedEvent(event);

        assertEquals(
                OrderStatus.CANCELLED,
                order.getStatus()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, times(1))
                .save(orderCaptor.capture());

        Order savedOrder =
                orderCaptor.getValue();

        assertEquals(
                orderId,
                savedOrder.getOrderId()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                savedOrder.getStatus()
        );
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForReservedEvent() {

        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        orderId,
                        UUID.randomUUID(),
                        2,
                        BigDecimal.valueOf(1000)
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> inventoryEventConsumer
                                .consumeInventoryReservedEvent(event)
                );

        assertEquals(
                "Order not found: " + orderId,
                exception.getMessage()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForFailedEvent() {

        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        InventoryFailedEvent event =
                new InventoryFailedEvent(
                        orderId,
                        "Inventory Error"
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> inventoryEventConsumer
                                .consumeInventoryFailedEvent(event)
                );

        assertEquals(
                "Order not found: " + orderId,
                exception.getMessage()
        );

        verify(orderRepository, times(1))
                .findById(orderId);

        verify(orderRepository, never())
                .save(any());
    }

}
