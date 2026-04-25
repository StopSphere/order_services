package com.order_service.shopsphere.order_service.Exception;

public class ExternalServiceException extends RuntimeException{
    public ExternalServiceException(String message){
        super(message);
    }
}
