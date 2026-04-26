package com.order_service.shopsphere.order_service.Service.Impl;

import com.order_service.shopsphere.order_service.Client.ProductClient;
import com.order_service.shopsphere.order_service.Client.StockRequest;
import com.order_service.shopsphere.order_service.Client.userClient;
import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.DTO.response.PagedResponse;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Exception.ExternalServiceException;
import com.order_service.shopsphere.order_service.Exception.OrderServiceException;
import com.order_service.shopsphere.order_service.Mapper.OrderMapper;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import com.order_service.shopsphere.order_service.Service.OrderService;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderMapper orderMapper;
    private final userClient userClient; //handling order with user id
    //logs
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

/*    @Value("${server.port}") //just for load balancer testing
    String server;*/
    @Override
    @CircuitBreaker(name = "productService", fallbackMethod = "handleProductServiceFailure")
    public OrderResponseDTO createOrder(CreateOrderRequestDTO requestDTO) {

        System.out.println("UserId from request: " + requestDTO.getUserId());
        userClient.getUserById(requestDTO.getUserId());
        productClient.reduceStock(requestDTO.getProductId(),new StockRequest(requestDTO.getQuantity()) );

        Order order = Order.builder()
                .userId(requestDTO.getUserId())
                .productId(requestDTO.getProductId())
                .quantity(requestDTO.getQuantity())
                .status(OrderStatus.CREATED)
                .build();
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }

    @Override
    public OrderResponseDTO getOrderById(UUID orderId) {
        Order order=orderRepository.findById(orderId)
                .orElseThrow(()-> new OrderServiceException("Order not found with id: "+orderId));
        return orderMapper.toResponseDTO(order);
    }

    @Override
    public PagedResponse<OrderResponseDTO> getAllOrders(int page, int size,String sortBy, String sortOrder, OrderStatus status, UUID productId) {
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();


        Pageable pageable = PageRequest.of(page, size,sort);

        Page<Order> orders ;
        if(status !=null){
            orders = orderRepository.findByStatus(status, pageable);
        }
        else if(productId!=null){
            orders = orderRepository.findByProductId(productId, pageable);
        }
        else {
            orders = orderRepository.findAll(pageable);
        }

        List<OrderResponseDTO> ordersDTO = orders.getContent()
                .stream()
                .map(orderMapper::toResponseDTO)
                .toList();

        return PagedResponse.<OrderResponseDTO>builder()
                .content(ordersDTO)
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .isLast(orders.isLast())
                .build();
    }

    public OrderResponseDTO handleProductServiceFailure(
            CreateOrderRequestDTO requestDTO,
            Throwable ex
    ) {
        System.out.println("Fallback triggered: " + ex.getMessage());

        Order order = Order.builder()
                .productId(requestDTO.getProductId())
                .quantity(requestDTO.getQuantity())
                .status(OrderStatus.CANCELLED)
                .build();

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponseDTO(savedOrder);
    }
}
