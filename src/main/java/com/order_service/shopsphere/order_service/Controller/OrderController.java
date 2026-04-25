package com.order_service.shopsphere.order_service.Controller;

import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.Service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody @Valid CreateOrderRequestDTO requestDTO){
        OrderResponseDTO responseDTO=orderService.createOrder(requestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
