package com.order_service.shopsphere.order_service.Service;

import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.DTO.response.PagedResponse;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;

import java.util.UUID;

public interface OrderService {

    public OrderResponseDTO createOrder(CreateOrderRequestDTO requestDTO);

    OrderResponseDTO getOrderById(UUID orderId);

    PagedResponse<OrderResponseDTO> getAllOrders(int page, int size , String sortBy, String sortOrder, OrderStatus status, UUID productId);
}
