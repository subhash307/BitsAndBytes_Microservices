package com.ecom.order_service.event;

import com.ecom.order_service.dto.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private double totalAmount;
//    private OrderStatus status;
}
