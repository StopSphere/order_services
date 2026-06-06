package com.order_service.shopsphere.order_service.Service.Impl;

import com.order_service.shopsphere.order_service.Client.InventoryClient;
import com.order_service.shopsphere.order_service.DTO.Event.OrderCreatedEvent;
import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.DTO.response.PagedResponse;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Exception.OrderServiceException;
import com.order_service.shopsphere.order_service.Kafka.OrderEventProducer;
import com.order_service.shopsphere.order_service.Mapper.OrderMapper;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import com.order_service.shopsphere.order_service.Service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    //shifted from feign to event driven architecture
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer producer;

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "handleInventoryFailure")
    public OrderResponseDTO createOrder(CreateOrderRequestDTO requestDTO, UUID userId) {

        System.out.println("Received create order request: " + requestDTO + " for user: " + userId);
        // Step 1: validate userId (JWT se aaya)
        if (userId == null) {
            throw new OrderServiceException("UserId is required");
        }

        logger.info("Creating order for user: {}", userId);

        // Step 2: Create order object
        System.out.println("Creating order object for user: " + userId);
        Order order = Order.builder()
                .userId(userId)   //FIXED
                .productId(requestDTO.getProductId())
                .quantity(requestDTO.getQuantity())
                .status(OrderStatus.CREATED)
                .build();

        System.out.println("Order object created: " + order);
        try {
            // Step 3: Call inventory service

            // Step 4: Save order
            Order savedOrder = orderRepository.save(order);
            logger.info("Order created successfully with id: {}", savedOrder.getOrderId());

            // kafka publisher
            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getProductId(),
                    savedOrder.getQuantity()
            );
            producer.sendOrderCreatedEvent(event);
            System.out.println("Order created and event published: " + event);

            return orderMapper.toResponseDTO(savedOrder);

        } catch (Exception e) {

            logger.error("Order failed for user {}: {}", userId, e.getMessage());

            throw new OrderServiceException("Order failed due to inventory issue");
        }
    }


    @Override
    public PagedResponse<OrderResponseDTO> getAllOrders(int page, int size, String sortBy, String sortOrder, OrderStatus status, UUID productId) {

        logger.info("Fetching orders: page={}, size={}, sortBy={}, order={}", page, size, sortBy, sortOrder);
        Page<Order> orderPage;


        try {
            //  Sorting
            Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);

            // Filtering logic (IMPORTANT FIX)
            if (status != null && productId != null) {
                orderPage = orderRepository.findByStatusAndProductId(status, productId, pageable);
            } else if (status != null) {
                orderPage = orderRepository.findByStatus(status, pageable);
            } else if (productId != null) {
                orderPage = orderRepository.findByProductId(productId, pageable);
            } else {
                orderPage = orderRepository.findAll(pageable);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //  Mapping
        List<OrderResponseDTO> content = orderPage.getContent().stream().map(orderMapper::toResponseDTO).toList();

        //  Response
        return PagedResponse.<OrderResponseDTO>builder().content(content).page(orderPage.getNumber()).size(orderPage.getSize()).totalElements(orderPage.getTotalElements()).totalPages(orderPage.getTotalPages()).isLast(orderPage.isLast()).build();
    }

    @Override
    public OrderResponseDTO getOrderById(UUID orderId) {

        logger.info("Fetching order with id: {}", orderId);

        //  Validate input
        if (orderId == null) {
            throw new OrderServiceException("Order ID must not be null");
        }

        // Fetch from DB
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderServiceException("Order not found with id: " + orderId));

        // Map to DTO
        return orderMapper.toResponseDTO(order);

    }

    @Override
    public List<OrderResponseDTO> getOrdersByUserId(UUID userId) {
        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    public OrderResponseDTO handleInventoryFailure(
            CreateOrderRequestDTO requestDTO,
            UUID userId,
            Exception ex) {

        logger.error("Fallback triggered for user {}: {}", userId, ex.getMessage());

        throw new OrderServiceException("Inventory service is down. Please try again later.");
    }
}