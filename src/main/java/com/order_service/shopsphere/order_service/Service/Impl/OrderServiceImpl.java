package com.order_service.shopsphere.order_service.Service.Impl;

import com.order_service.shopsphere.order_service.Client.ProductClient;
import com.order_service.shopsphere.order_service.Client.StockRequest;
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

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderMapper orderMapper;
    //logs
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public OrderResponseDTO createOrder(CreateOrderRequestDTO requestDTO) {
        Order order=Order.builder()
                .productId(requestDTO.getProductId())
                .quantity(requestDTO.getQuantity())
                .status(OrderStatus.PROCESSING)
                .build();
        StockRequest stockRequest=StockRequest.builder()
                .quantity(requestDTO.getQuantity())
                .build();
        try{
            logger.info("Calling Product Service to reduce stock for productId: {}", requestDTO.getProductId());
            productClient.reduceStock(requestDTO.getProductId(), stockRequest);
            order.setStatus(OrderStatus.CREATED);
        }
        catch(FeignException e){
            logger.info("Feign exception caught" + e.getMessage());
           order.setStatus(OrderStatus.CANCELLED);
            if (e.status() == 404) {
                throw new ExternalServiceException("Product not found");
            } else if (e.status() == 400) {
                throw new ExternalServiceException("Insufficient stock");
            } else {
                throw new ExternalServiceException("Product service unavailable");
            }
        }

        Order savedOrder=orderRepository.save(order);
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
}
