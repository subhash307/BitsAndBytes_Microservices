package com.ecom.order_service.listener;

import com.ecom.order_service.dto.OrderStatus;
import com.ecom.order_service.event.PaymentCompletedEvent;
import com.ecom.order_service.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }
    @KafkaListener(topics="payment-events", groupId="order-service-group" )
    public void consume(String eventJson) throws JsonProcessingException {
        log.info("Received order event JSON: {}", eventJson);
        PaymentCompletedEvent event = objectMapper.readValue(eventJson, PaymentCompletedEvent.class);
        log.info("Deserialized OrderCreatedEvent: {}", event);
        orderService.updateOrderStatus(event.getOrderId(), Objects.equals(event.getPaymentStatus(), "SUCCESS") ? OrderStatus.CONFIRMED: OrderStatus.CANCELLED);
    }
}
