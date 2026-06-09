package com.order_service.shopsphere.order_service.Kafka;

import com.order_service.shopsphere.order_service.DTO.Event.PaymentFailedEvent;
import com.order_service.shopsphere.order_service.DTO.Event.PaymentSuccessEvent;
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
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "payment-success",
            groupId = "order-service-group",
            properties = {
        "spring.json.value.default.type=com.order_service.shopsphere.order_service.DTO.Event.PaymentSuccessEvent"
    }
    )
    @Transactional
    public void consumePaymentSuccess(
            PaymentSuccessEvent event) {

        Order order = orderRepository.findById(
                event.getOrderId()
        ).orElseThrow(
                () -> new RuntimeException(
                        "Order not found"
                )
        );

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        orderRepository.save(order);

        log.info(
                "Order Confirmed after payment: {}",
                order.getOrderId()
        );
    }

    @KafkaListener(
            topics = "payment-failed",
            groupId = "order-service-group",
            properties = {
                    "spring.json.value.default.type=com.order_service.shopsphere.order_service.DTO.Event.PaymentFailedEvent"
            }

    )
    @Transactional
    public void consumePaymentFailed(
            PaymentFailedEvent event) {

        Order order = orderRepository.findById(
                event.getOrderId()
        ).orElseThrow(
                () -> new RuntimeException(
                        "Order not found"
                )
        );

        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);

        log.info(
                "Order Cancelled due to payment failure: {}",
                order.getOrderId()
        );
    }
}