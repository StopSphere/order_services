package com.order_service.shopsphere.order_service.Service;

import com.order_service.shopsphere.order_service.DTO.Event.OrderCreatedEvent;
import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Exception.OrderServiceException;
import com.order_service.shopsphere.order_service.Kafka.OrderEventProducer;
import com.order_service.shopsphere.order_service.Mapper.OrderMapper;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import com.order_service.shopsphere.order_service.Service.Impl.OrderServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    @Test
    void shouldCreateOrderSuccessfully() {

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        productId,
                        2
                );

        Order savedOrder = new Order();
        savedOrder.setOrderId(UUID.randomUUID());
        savedOrder.setUserId(userId);
        savedOrder.setProductId(productId);
        savedOrder.setQuantity(2);
        savedOrder.setStatus(OrderStatus.CREATED);

        OrderResponseDTO responseDTO =
                new OrderResponseDTO();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        when(orderMapper.toResponseDTO(savedOrder))
                .thenReturn(responseDTO);

        OrderResponseDTO result =
                orderService.createOrder(
                        request,
                        userId
                );

        assertEquals(
                responseDTO,
                result
        );

        verify(orderRepository)
                .save(any(Order.class));

        verify(orderEventProducer)
                .sendOrderCreatedEvent(
                        eventCaptor.capture()
                );

        OrderCreatedEvent event =
                eventCaptor.getValue();

        assertEquals(
                productId,
                event.getProductId()
        );

        assertEquals(
                2,
                event.getQuantity()
        );
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {

        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        UUID.randomUUID(),
                        2
                );

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.createOrder(
                                request,
                                null
                        )
                );

        assertEquals(
                "UserId is required",
                exception.getMessage()
        );

        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldThrowExceptionWhenKafkaPublishFails() {

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        productId,
                        2
                );

        Order savedOrder = new Order();

        savedOrder.setOrderId(UUID.randomUUID());
        savedOrder.setUserId(userId);
        savedOrder.setProductId(productId);
        savedOrder.setQuantity(2);
        savedOrder.setStatus(OrderStatus.CREATED);

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        doThrow(
                new RuntimeException(
                        "Kafka failure"
                )
        ).when(orderEventProducer)
                .sendOrderCreatedEvent(any());

        assertThrows(
                OrderServiceException.class,
                () -> orderService.createOrder(
                        request,
                        userId
                )
        );
    }

    @Test
    void shouldGetOrderByIdSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);

        OrderResponseDTO response =
                new OrderResponseDTO();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderMapper.toResponseDTO(order))
                .thenReturn(response);

        OrderResponseDTO result =
                orderService.getOrderById(orderId);

        assertEquals(
                response,
                result
        );

        verify(orderRepository)
                .findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.getOrderById(orderId)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Order not found")
        );
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.getOrderById(null)
                );

        assertEquals(
                "Order ID must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowInventoryDownException() {

        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        UUID.randomUUID(),
                        2
                );

        UUID userId = UUID.randomUUID();

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.handleInventoryFailure(
                                request,
                                userId,
                                new RuntimeException(
                                        "Inventory Down"
                                )
                        )
                );

        assertEquals(
                "Inventory service is down. Please try again later.",
                exception.getMessage()
        );
    }
}