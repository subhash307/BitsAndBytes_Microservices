package com.ecom.payment_service.service;

import com.ecom.payment_service.dto.PaymentRequestDTO;
import com.ecom.payment_service.dto.PaymentResponseDTO;
import com.ecom.payment_service.dto.PaymentStatus;
import com.ecom.payment_service.entity.Payment;
import com.ecom.payment_service.event.PaymentCompletedEvent;
import com.ecom.payment_service.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public PaymentService(PaymentRepository paymentRepository, OrderClient orderClient, KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate) {
//        this.paymentRepository = paymentRepository;
//        this.orderClient = orderClient;
//        this.kafkaTemplate = kafkaTemplate;
//
//    }

    public PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO) throws JsonProcessingException {
        String paymentId = generatePaymentId();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrderId(paymentRequestDTO.getOrderId());
        payment.setCustomerId(paymentRequestDTO.getCustomerId());
        payment.setAmount(paymentRequestDTO.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        boolean paymentSuccess = new Random().nextBoolean();
        if( paymentSuccess ) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(UUID.randomUUID().toString());
//            orderClient.updateOrderStatus(paymentRequestDTO.getOrderId(), "CONFIRMED");
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setTransactionId("N/A");
//            orderClient.updateOrderStatus(paymentRequestDTO.getOrderId(), "CANCELLED");
        }
        paymentRepository.save(payment);
        updatePaymentStatus(payment, payment.getPaymentStatus().name()); // Notify order service
        PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
        paymentResponseDTO.setPaymentId(paymentId);
        paymentResponseDTO.setOrderId(payment.getOrderId());
        paymentResponseDTO.setCustomerId(payment.getCustomerId());
        paymentResponseDTO.setAmount(payment.getAmount());
        paymentResponseDTO.setPaymentStatus(payment.getPaymentStatus());
        paymentResponseDTO.setPaymentDate(LocalDateTime.now());
        paymentResponseDTO.setTransactionId(payment.getTransactionId());
        return paymentResponseDTO;

    }

    public PaymentResponseDTO getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            return null; // or throw an exception
        }
        PaymentResponseDTO paymentResponseDTO = new PaymentResponseDTO();
        paymentResponseDTO.setPaymentId(payment.getPaymentId());
        paymentResponseDTO.setOrderId(payment.getOrderId());
        paymentResponseDTO.setAmount(payment.getAmount());
        paymentResponseDTO.setPaymentStatus(payment.getPaymentStatus());
        paymentResponseDTO.setTransactionId(payment.getTransactionId());
        return paymentResponseDTO;
    }

    public void updatePaymentStatus(Payment payment, String status) throws JsonProcessingException {
        // After payment, publish PaymentCompletedEvent
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                status,
                payment.getTransactionId()
        );
        String eventJson = objectMapper.writeValueAsString(completedEvent);
        kafkaTemplate.send("payment-events", eventJson);
    }
    private String generatePaymentId() {
        return "pay-" + UUID.randomUUID().toString().substring(0, 8);
    }


}
