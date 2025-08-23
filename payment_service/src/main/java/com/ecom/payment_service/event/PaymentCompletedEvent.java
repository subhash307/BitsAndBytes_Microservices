package com.ecom.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private double amount;
    private String paymentStatus;
    private String transactionId;
}
