package com.order_service.shopsphere.order_service.Client;

import com.order_service.shopsphere.order_service.DTO.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name="user-service")
public interface userClient {

    @GetMapping("/v1/api/users/{id}")
    UserResponse getUserById(@PathVariable UUID id);
}
