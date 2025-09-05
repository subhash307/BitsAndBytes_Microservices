package com.ecom.payment_service.listener;

import com.ecom.payment_service.dto.PaymentRequestDTO;
import com.ecom.payment_service.events.OrderCreatedEvent;
import com.ecom.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics="order-events", groupId="payment-service-group")
    public void consume(String message) {
        try {
            log.info("Received message: {}", message);
            OrderCreatedEvent orderCreatedEvent = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("Deserialized OrderCreatedEvent: {}", orderCreatedEvent);

            PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
            paymentRequestDTO.setOrderId(orderCreatedEvent.getOrderId());
            paymentRequestDTO.setCustomerId(orderCreatedEvent.getCustomerId());
            paymentRequestDTO.setAmount(Double.parseDouble(orderCreatedEvent.getTotalAmount()));

            paymentService.processPayment(paymentRequestDTO);
            log.info("Payment processed for Order ID: {}", orderCreatedEvent.getOrderId());
        } catch(Exception e) {
            log.error("Error processing OrderCreatedEvent: {}", e.getMessage());
            throw new RuntimeException("Error processing OrderCreatedEvent", e);
        }
    }
}
