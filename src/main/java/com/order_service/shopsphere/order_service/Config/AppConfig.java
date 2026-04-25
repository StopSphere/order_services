package com.order_service.shopsphere.order_service.Config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Client feignClient() {
        return new ApacheHttpClient();
    }
}
