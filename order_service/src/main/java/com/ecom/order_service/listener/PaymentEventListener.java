package com.ecom.order_service.listener;

import com.ecom.order_service.dto.OrderStatus;
import com.ecom.order_service.events.PaymentCompletedEvent;
import com.ecom.order_service.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics="payment-events", groupId="order-service-group")
    public void consume(String message) {

        try {
            log.info("Received payment event message: {}", message);
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            log.info("Deserialized PaymentCompletedEvent: {}", event);
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.valueOf(event.getStatus()));
            log.info("Order {} status updated to {}", event.getOrderId(), event.getStatus());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing payment event message: {}", e.getMessage());
            throw new RuntimeException("Error deserializing payment event message", e);
        }
        catch(Exception e) {
            log.error("Error updating order status : {}",  e.getMessage());
            throw new RuntimeException("Error updating order status", e);
        }
    }
}
