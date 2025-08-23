package com.ecom.payment_service.listener;

import com.ecom.payment_service.dto.PaymentRequestDTO;
import com.ecom.payment_service.event.OrderCreatedEvent;
import com.ecom.payment_service.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public void consume(String eventJson) {
        try {
            log.info("Received order event JSON: {}", eventJson);
            OrderCreatedEvent event = objectMapper.readValue(eventJson, OrderCreatedEvent.class);
            log.info("Deserialized OrderCreatedEvent: {}", event);

            PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
            paymentRequest.setOrderId(event.getOrderId());
            paymentRequest.setAmount(event.getTotalAmount());
            paymentRequest.setCustomerId(event.getCustomerId());

            paymentService.processPayment(paymentRequest);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing order event", e);
            throw new RuntimeException("Error processing order event", e);
        }
    }
}
