package com.order_service.shopsphere.order_service.Repository;

import com.order_service.shopsphere.order_service.Entity.Order;
import com.order_service.shopsphere.order_service.Entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByProductId(UUID productId, Pageable pageable);

    List<Order> findByUserId(UUID userId);

    Page<Order> findByStatusAndProductId(OrderStatus status, UUID productId, Pageable pageable);
}
