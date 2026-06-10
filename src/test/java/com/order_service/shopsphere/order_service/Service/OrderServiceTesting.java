package com.order_service.shopsphere.order_service.Service;

import com.order_service.shopsphere.order_service.Client.ProductClient;
import com.order_service.shopsphere.order_service.DTO.Event.OrderCreatedEvent;
import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.DTO.response.ProductResponse;
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

import java.math.BigDecimal;
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
    private OrderEventProducer producer;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    void shouldCreateOrderSuccessfully() {

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CreateOrderRequestDTO request =
                new CreateOrderRequestDTO(
                        productId,
                        2
                );

        ProductResponse productResponse =
                new ProductResponse();

        productResponse.setPrice(
                BigDecimal.valueOf(100)
        );

        Order savedOrder = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .totalAmount(BigDecimal.valueOf(200))
                .status(OrderStatus.CREATED)
                .build();

        OrderResponseDTO responseDTO =
                new OrderResponseDTO();

        when(
                productClient.getProduct(productId)
        ).thenReturn(productResponse);

        when(
                orderRepository.save(any(Order.class))
        ).thenReturn(savedOrder);

        when(
                orderMapper.toResponseDTO(savedOrder)
        ).thenReturn(responseDTO);

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
                .save(orderCaptor.capture());

        Order capturedOrder =
                orderCaptor.getValue();

        assertEquals(
                userId,
                capturedOrder.getUserId()
        );

        assertEquals(
                productId,
                capturedOrder.getProductId()
        );

        assertEquals(
                2,
                capturedOrder.getQuantity()
        );

        assertEquals(
                BigDecimal.valueOf(200),
                capturedOrder.getTotalAmount()
        );

        assertEquals(
                OrderStatus.CREATED,
                capturedOrder.getStatus()
        );

        verify(producer)
                .sendOrderCreatedEvent(
                        eventCaptor.capture()
                );

        OrderCreatedEvent event =
                eventCaptor.getValue();

        assertEquals(
                orderId,
                event.getOrderId()
        );

        assertEquals(
                productId,
                event.getProductId()
        );

        assertEquals(
                2,
                event.getQuantity()
        );

        assertEquals(
                BigDecimal.valueOf(200),
                event.getAmount()
        );

        verify(orderMapper)
                .toResponseDTO(savedOrder);
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
        verifyNoInteractions(producer);
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

        ProductResponse productResponse =
                new ProductResponse();

        productResponse.setPrice(
                BigDecimal.valueOf(100)
        );

        Order savedOrder = Order.builder()
                .orderId(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .totalAmount(BigDecimal.valueOf(200))
                .status(OrderStatus.CREATED)
                .build();

        when(
                productClient.getProduct(productId)
        ).thenReturn(productResponse);

        when(
                orderRepository.save(any(Order.class))
        ).thenReturn(savedOrder);

        doThrow(
                new RuntimeException("Kafka failure")
        ).when(producer)
                .sendOrderCreatedEvent(any());

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.createOrder(
                                request,
                                userId
                        )
                );

        assertEquals(
                "Order failed due to inventory issue",
                exception.getMessage()
        );

        verify(producer)
                .sendOrderCreatedEvent(any());
    }

    @Test
    void shouldGetOrderByIdSuccessfully() {

        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);

        OrderResponseDTO response =
                new OrderResponseDTO();

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.of(order));

        when(
                orderMapper.toResponseDTO(order)
        ).thenReturn(response);

        OrderResponseDTO result =
                orderService.getOrderById(orderId);

        assertEquals(
                response,
                result
        );

        verify(orderRepository)
                .findById(orderId);

        verify(orderMapper)
                .toResponseDTO(order);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        UUID orderId = UUID.randomUUID();

        when(
                orderRepository.findById(orderId)
        ).thenReturn(Optional.empty());

        OrderServiceException exception =
                assertThrows(
                        OrderServiceException.class,
                        () -> orderService.getOrderById(orderId)
                );

        assertEquals(
                "Order not found with id: " + orderId,
                exception.getMessage()
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
