package com.order_service.shopsphere.order_service.Mapper;

import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.Entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponseDTO toResponseDTO(Order order);
    Order toOrder(OrderResponseDTO orderResponseDTO);
}
