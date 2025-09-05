package com.ecom.payment_service.service;

import com.ecom.payment_service.dto.PaymentRequestDTO;
import com.ecom.payment_service.dto.PaymentResponseDTO;
import com.ecom.payment_service.dto.PaymentStatus;
import com.ecom.payment_service.entity.Payment;
import com.ecom.payment_service.events.PaymentCompletedEvent;
import com.ecom.payment_service.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
//    private OrderClient orderClient;



    public PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO) {
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
           // orderClient.updateOrderStatus(paymentRequestDTO.getOrderId(), "CONFIRMED");
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setTransactionId("N/A");
          //  orderClient.updateOrderStatus(paymentRequestDTO.getOrderId(), "CANCELLED");
        }
        paymentRepository.save(payment);
        String status = paymentSuccess ? "CONFIRMED" : "CANCELLED";
        log.info("Payment {} successfully processed", paymentId);
        updatePaymentStatus(paymentRequestDTO.getOrderId(), status);
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

    public void updatePaymentStatus(String orderId, String status) {
        try {
            log.info("Updating payment status for payment {}", orderId);
            PaymentCompletedEvent event = new PaymentCompletedEvent(orderId, status);
            String eventString = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("payment-events", eventString);
            log.info("Payment status update event sent to Kafka for payment {}", orderId);
        } catch (Exception e) {
            log.error("Failed to update payment status for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to update payment status", e);
        }


    }
    private String generatePaymentId() {
        return "pay-" + UUID.randomUUID().toString().substring(0, 8);
    }


}
