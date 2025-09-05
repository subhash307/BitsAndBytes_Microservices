package com.ecom.order_service.service;

import com.ecom.order_service.dto.*;
import com.ecom.order_service.entity.Orders;
import com.ecom.order_service.entity.OrderItem;
import com.ecom.order_service.events.OrderCreatedEvent;
import com.ecom.order_service.repository.OrderItemRepository;
import com.ecom.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;


    public OrderResponseDTO placeOrder(OrderRequestDTO requestDTO) {

        // Generate OrderID
        String orderId = generateOrderId();
        double totalAmount = 0.0;

        List<OrderItem> orderItems = new ArrayList<>();
        for(OrderItemRequestDTO itemRequest : requestDTO.getItems()) {
            ProductResponseDTO product = productClient.getProductName(itemRequest.getProductId());
            if(product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            productClient.updateStock(itemRequest.getProductId(), -itemRequest.getQuantity());

            double itemTotal = itemRequest.getQuantity() * product.getPrice();
            totalAmount += itemTotal;

            OrderItem orderItem = new OrderItem(generateOrderItemId(), orderId,
                    itemRequest.getProductId(), itemRequest.getQuantity(), product.getPrice());
            orderItems.add(orderItem);
        }

        Orders order = new Orders(orderId, requestDTO.getCustomerId(),
                LocalDateTime.now(), totalAmount, OrderStatus.PENDING);
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        // kafka event
        placeOrder(order);

        return new OrderResponseDTO(order.getOrderId(), order.getCustomerId(),
                order.getOrderDate(), order.getTotalAmount(), order.getStatus(), orderItems);
    }

    public OrderResponseDTO getOrderById(String orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return new OrderResponseDTO(order.getOrderId(), order.getCustomerId(),
                order.getOrderDate(), order.getTotalAmount(), order.getStatus(), items);
    }

    public List<OrderResponseDTO> getOrdersByCustomerId(String customerId) {
        List<Orders> orders = orderRepository.findByCustomerId(customerId);
        List<OrderResponseDTO> responseList = new ArrayList<>();

        for (Orders order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            responseList.add(new OrderResponseDTO(order.getOrderId(), order.getCustomerId(),
                    order.getOrderDate(), order.getTotalAmount(), order.getStatus(), items));
        }
        return responseList;
    }

    public void updateOrderStatus(String orderId, OrderStatus orderStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        order.setStatus(orderStatus);
        orderRepository.save(order);
    }

    public void placeOrder(Orders order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getOrderId(),
                    order.getCustomerId(),
                    String.valueOf(order.getTotalAmount())
            );
            log.info("Sending OrderCreatedEvent to Kafka for order ID: {}", order.getOrderId());
            kafkaTemplate.send("order-events", event);
            log.info("OrderCreatedEvent sent to Kafka for order ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order created event to Kafka for order ID: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to send order created event", e);
        }
    }

    private String generateOrderId() {
        return "ord-" + UUID.randomUUID().toString().substring(0, 8);
    }
    private String generateOrderItemId() {
        return "item-" + UUID.randomUUID().toString().substring(0, 8);
    }

}
