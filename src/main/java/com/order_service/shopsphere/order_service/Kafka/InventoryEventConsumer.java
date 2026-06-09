package com.order_service.shopsphere.order_service.Kafka;

import com.order_service.shopsphere.order_service.DTO.Event.InventoryFailedEvent;
import com.order_service.shopsphere.order_service.DTO.Event.InventoryReservedEvent;
import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import com.order_service.shopsphere.order_service.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    private final OrderRepository orderRepository;

//    @KafkaListener(topics = "inventory-reserved", groupId = "order-service-group")
//    @Transactional
//    public void consumeInventoryReservedEvent(InventoryReservedEvent event) {
//        try {
//            Order order = orderRepository.findById(event.getOrderId())
//                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
//
//            order.setStatus(OrderStatus.CONFIRMED);
//            orderRepository.save(order);
//
//            log.info("Order Confirmed: {}", order.getOrderId());
//        } catch (Exception ex) {
//            log.error("Failed to confirm order {}: {}", event.getOrderId(), ex.getMessage());
//            throw ex;
//        }
//    }

    @KafkaListener(topics = "inventory-reserved", groupId = "order-service-group",
            properties = {
                    "spring.json.value.default.type=com.order_service.shopsphere.order_service.DTO.Event.InventoryReservedEvent"
            }
    )
    @Transactional
    public void consumeInventoryReservedEvent(
            InventoryReservedEvent event) {

        log.info(
                "Inventory reserved for order: {}",
                event.getOrderId()
        );
    }

    @KafkaListener(topics = "inventory-failed", groupId = "order-service-group"
    ,properties = {
            "spring.json.value.default.type=com.order_service.shopsphere.order_service.DTO.Event.InventoryFailedEvent"
    })
    @Transactional
    public void consumeInventoryFailedEvent(InventoryFailedEvent event) {
        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.info("Order Cancelled: {} - Reason: {}", order.getOrderId(), event.getReason());
        } catch (Exception ex) {
            log.error("Failed to cancel order {}: {}", event.getOrderId(), ex.getMessage());
            throw ex;
        }
    }
}
