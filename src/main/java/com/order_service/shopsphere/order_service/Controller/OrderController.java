package com.order_service.shopsphere.order_service.Controller;

import com.order_service.shopsphere.order_service.DTO.request.CreateOrderRequestDTO;
import com.order_service.shopsphere.order_service.DTO.response.OrderResponseDTO;
import com.order_service.shopsphere.order_service.DTO.response.PagedResponse;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID orderId){
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponseDTO>> getAllOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5" ) int size, @RequestParam(defaultValue = "orderId" ) String sortBy, @RequestParam(defaultValue = "asc") String sortOrder,
                                                                        @RequestParam(required = false) OrderStatus status, @RequestParam(required = false) UUID productId){
        return  ResponseEntity.ok(orderService.getAllOrders(page, size, sortBy, sortOrder,status,productId));
    }


}
