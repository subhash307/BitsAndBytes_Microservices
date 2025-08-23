package com.ecom.payment_service.controller;

import com.ecom.payment_service.dto.PaymentRequestDTO;
import com.ecom.payment_service.dto.PaymentResponseDTO;
import com.ecom.payment_service.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(@RequestBody PaymentRequestDTO paymentRequestDTO) throws JsonProcessingException {
        return ResponseEntity.ok(paymentService.processPayment(paymentRequestDTO));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentDetails(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

}
