package com.medibook.payment.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PaymentResourceTest {

    @Mock private PaymentService paymentService;

    @InjectMocks private PaymentResource controller;

    private PaymentResponse sampleResponse;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        sampleResponse = PaymentResponse.builder()
                .paymentId(1).appointmentId(10).status("SUCCESS")
                .amount(500.0).currency("INR").paymentMethod("UPI")
                .razorpayOrderId("order_1").razorpayPaymentId("pay_1")
                .message("Payment successful.").transactionTime("2026-01-01 10:00:00")
                .build();

        samplePayment = Payment.builder()
                .paymentId(1).appointmentId(10).patientId(2)
                .amount(500.0).currency("INR").paymentMethod("UPI")
                .status("SUCCESS").razorpayOrderId("order_1")
                .createdAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("POST /payments/initiate")
    class InitiatePaymentEndpoint {

        @Test
        @DisplayName("returns 201 Created on successful payment initiation")
        void initiatePayment_returns201() {
            PaymentRequest req = new PaymentRequest();
            req.setAppointmentId(10);
            req.setPatientId(2);
            req.setAmount(500.0);
            req.setPaymentMethod("UPI");

            when(paymentService.initiatePayment(req)).thenReturn(sampleResponse);

            ResponseEntity<PaymentResponse> response = controller.initiatePayment(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(sampleResponse);
        }
    }

    @Nested
    @DisplayName("POST /payments/verify")
    class VerifyPaymentEndpoint {

        @Test
        @DisplayName("returns 200 OK with successful verification")
        void verifyPayment_returns200() {
            Map<String, String> body = new HashMap<>();
            body.put("razorpayOrderId", "order_1");
            body.put("razorpayPaymentId", "pay_1");
            body.put("razorpaySignature", "sig_1");

            when(paymentService.verifyPayment("order_1", "pay_1", "sig_1"))
                    .thenReturn(sampleResponse);

            ResponseEntity<PaymentResponse> response = controller.verifyPayment(body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("handles null signature in body (mock mode)")
        void verifyPayment_nullSignature() {
            Map<String, String> body = new HashMap<>();
            body.put("razorpayOrderId", "MOCK_ORDER_1");
            body.put("razorpayPaymentId", "MOCK_PAY_1");

            when(paymentService.verifyPayment("MOCK_ORDER_1", "MOCK_PAY_1", null))
                    .thenReturn(sampleResponse);

            ResponseEntity<PaymentResponse> response = controller.verifyPayment(body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /payments/appointment/{appointmentId}")
    class GetByAppointmentEndpoint {

        @Test
        @DisplayName("returns 200 OK with payment for given appointmentId")
        void getByAppointment_returns200() {
            when(paymentService.getPaymentByAppointment(10)).thenReturn(sampleResponse);

            ResponseEntity<PaymentResponse> response = controller.getByAppointment(10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getAppointmentId()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("GET /payments/{paymentId}")
    class GetByIdEndpoint {

        @Test
        @DisplayName("returns 200 OK with payment for given paymentId")
        void getById_returns200() {
            when(paymentService.getPaymentById(1)).thenReturn(sampleResponse);

            ResponseEntity<PaymentResponse> response = controller.getById(1);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getPaymentId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("GET /payments/patient/{patientId}")
    class GetByPatientEndpoint {

        @Test
        @DisplayName("returns 200 OK with list of payments for patient")
        void getByPatient_returns200() {
            when(paymentService.getPaymentsByPatient(2)).thenReturn(List.of(samplePayment));

            ResponseEntity<List<Payment>> response = controller.getByPatient(2);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("returns 200 OK with empty list when no payments for patient")
        void getByPatient_emptyList() {
            when(paymentService.getPaymentsByPatient(99)).thenReturn(List.of());

            ResponseEntity<List<Payment>> response = controller.getByPatient(99);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /payments/{paymentId}/refund")
    class InitiateRefundEndpoint {

        @Test
        @DisplayName("returns 200 OK with refund response")
        void initiateRefund_returns200() {
            PaymentResponse refundResponse = PaymentResponse.builder()
                    .paymentId(1).status("REFUNDED").message("Refund initiated successfully.")
                    .build();

            when(paymentService.initiateRefund(1)).thenReturn(refundResponse);

            ResponseEntity<PaymentResponse> response = controller.initiateRefund(1);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isEqualTo("REFUNDED");
        }
    }

    @Nested
    @DisplayName("GET /payments/status?status=")
    class GetByStatusEndpoint {

        @Test
        @DisplayName("returns 200 OK with filtered payments by SUCCESS status")
        void getByStatus_success_returns200() {
            when(paymentService.getPaymentsByStatus("SUCCESS")).thenReturn(List.of(samplePayment));

            ResponseEntity<List<Payment>> response = controller.getByStatus("SUCCESS");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("returns 200 OK with empty list for PENDING status")
        void getByStatus_pending_returns200() {
            when(paymentService.getPaymentsByStatus("PENDING")).thenReturn(List.of());

            ResponseEntity<List<Payment>> response = controller.getByStatus("PENDING");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /payments/revenue/total")
    class GetTotalRevenueEndpoint {

        @Test
        @DisplayName("returns 200 OK with total revenue map")
        void getTotalRevenue_returns200() {
            when(paymentService.getTotalRevenue()).thenReturn(15000.0);

            ResponseEntity<?> response = controller.getTotalRevenue();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body).containsKey("totalRevenue");
            assertThat(body.get("totalRevenue")).isEqualTo(15000.0);
            assertThat(body.get("currency")).isEqualTo("INR");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getRevenueByProvider()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /payments/revenue/provider/{providerId}")
    class GetRevenueByProviderTests {

        @Test
        @DisplayName("returns 200 OK with revenue map for given providerId")
        void getRevenueByProvider_returns200() {
            when(paymentService.getRevenueByProvider(5)).thenReturn(8500.0);

            ResponseEntity<?> response = controller.getRevenueByProvider(5);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("providerId")).isEqualTo(5);
            assertThat(body.get("providerRevenue")).isEqualTo(8500.0);
            assertThat(body.get("currency")).isEqualTo("INR");
        }

        @Test
        @DisplayName("returns 200 OK with zero revenue when no payments exist")
        void getRevenueByProvider_zeroRevenue() {
            when(paymentService.getRevenueByProvider(99)).thenReturn(0.0);

            ResponseEntity<?> response = controller.getRevenueByProvider(99);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("providerRevenue")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns 200 OK with correct providerId in body")
        void getRevenueByProvider_bodyContainsProviderId() {
            when(paymentService.getRevenueByProvider(10)).thenReturn(12000.0);

            ResponseEntity<?> response = controller.getRevenueByProvider(10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("providerId")).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("GET /payments/provider/{providerId}")
    class GetByProviderEndpoint {

        @Test
        @DisplayName("returns 200 OK with list of payments for provider")
        void getByProvider_returns200() {
            when(paymentService.getPaymentsByProvider(5)).thenReturn(List.of(samplePayment));

            ResponseEntity<?> response = controller.getPaymentsByProvider(5);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("returns 500 when service throws RuntimeException")
        void getByProvider_serviceThrows_returns500() {
            when(paymentService.getPaymentsByProvider(5))
                    .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<?> response = controller.getPaymentsByProvider(5);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("PUT /payments/{paymentId}/status")
    class UpdateStatusEndpoint {

        @Test
        @DisplayName("returns 200 OK with success message after status update")
        void updateStatus_returns200() {
            // updatePaymentStatus is void
            ResponseEntity<?> response = controller.updateStatus(1, "REFUNDED");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("message").toString()).contains("REFUNDED");
            assertThat(body.get("paymentId")).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 200 OK with correct paymentId in body")
        void updateStatus_bodyContainsPaymentId() {
            ResponseEntity<?> response = controller.updateStatus(42, "SUCCESS");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body.get("paymentId")).isEqualTo(42);
            assertThat(body.get("message").toString()).contains("SUCCESS");
        }   
    }
}
