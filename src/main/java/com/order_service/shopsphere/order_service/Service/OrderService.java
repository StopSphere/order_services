package com.order_service.shopsphere.order_service.Service;

import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.Entity.Order;

public interface OrderService {

    public OrderResponseDTO createOrder(CreateOrderRequestDTO requestDTO);
}
