package com.medibook.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.json.JSONObject;
import org.springframework.test.util.ReflectionTestUtils;

import com.medibook.payment.client.AppointmentClient;
import com.medibook.payment.dto.request.AppointmentDto;
import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.BadRequestException;
import com.medibook.payment.exception.DuplicateResourceException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository  paymentRepository;
    @Mock private AppointmentClient  appointmentClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private AppointmentDto scheduledAppointment;
    private AppointmentDto pendingPaymentAppointment;
    private AppointmentDto cancelledAppointment;
    private AppointmentDto completedAppointment;
    private Payment pendingPayment;
    private Payment successPayment;
    private Payment failedPayment;
    private Payment refundedPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId",     "mock_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "mock_secret");
        ReflectionTestUtils.setField(paymentService, "razorpayCurrency",  "INR");

        scheduledAppointment = new AppointmentDto();
        scheduledAppointment.setAppointmentId(1);
        scheduledAppointment.setPatientId(2);
        scheduledAppointment.setStatus("SCHEDULED");

        pendingPaymentAppointment = new AppointmentDto();
        pendingPaymentAppointment.setAppointmentId(3);
        pendingPaymentAppointment.setPatientId(2);
        pendingPaymentAppointment.setStatus("PENDING_PAYMENT");

        cancelledAppointment = new AppointmentDto();
        cancelledAppointment.setAppointmentId(4);
        cancelledAppointment.setStatus("CANCELLED");

        completedAppointment = new AppointmentDto();
        completedAppointment.setAppointmentId(2);
        completedAppointment.setStatus("COMPLETED");

        pendingPayment = Payment.builder()
                .paymentId(100).appointmentId(1).patientId(2)
                .amount(500.0).currency("INR").paymentMethod("CASH")
                .status("PENDING").razorpayOrderId("MOCK_ORDER_1")
                .razorpayPaymentId(null).notes("Pay at clinic")
                .createdAt(LocalDateTime.now()).build();

        successPayment = Payment.builder()
                .paymentId(101).appointmentId(1).patientId(2)
                .amount(500.0).currency("INR").paymentMethod("UPI")
                .status("SUCCESS").razorpayOrderId("MOCK_ORDER_1")
                .razorpayPaymentId("TXN_MOCK_PAY_1").razorpaySignature("valid_sig")
                .createdAt(LocalDateTime.now()).build();

        failedPayment = Payment.builder()
                .paymentId(102).appointmentId(1).patientId(2)
                .amount(500.0).currency("INR").paymentMethod("CARD")
                .status("FAILED").razorpayOrderId("MOCK_ORDER_FAIL")
                .createdAt(LocalDateTime.now()).build();

        refundedPayment = Payment.builder()
                .paymentId(103).appointmentId(5).patientId(2)
                .amount(300.0).currency("INR").paymentMethod("UPI")
                .status("REFUNDED").razorpayOrderId("MOCK_ORDER_5")
                .razorpayPaymentId("TXN_REFUND_1").createdAt(LocalDateTime.now()).build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private PaymentRequest buildRequest(int appointmentId) {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(appointmentId);
        req.setPatientId(2);
        req.setAmount(500.0);
        req.setCurrency("INR");
        req.setPaymentMethod("CASH");
        return req;
    }

    private Order buildOrder(String orderId) throws Exception {
        return new Order(new JSONObject().put("id", orderId));
    }

    private Refund buildRefund(String status) throws Exception {
        return new Refund(new JSONObject().put("status", status));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // initiatePayment()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("initiatePayment()")
    class InitiatePaymentTests {

        @Test
        @DisplayName("throws BadRequestException when appointment is CANCELLED")
        void cancelledAppointment_throws() {
            when(appointmentClient.getById(4)).thenReturn(cancelledAppointment);

            assertThatThrownBy(() -> paymentService.initiatePayment(buildRequest(4)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("CANCELLED");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException when appointment is COMPLETED")
        void completedAppointment_throws() {
            when(appointmentClient.getById(2)).thenReturn(completedAppointment);

            assertThatThrownBy(() -> paymentService.initiatePayment(buildRequest(2)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("COMPLETED");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when payment already SUCCESS")
        void alreadySuccessPayment_throws() {
            when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(successPayment));

            assertThatThrownBy(() -> paymentService.initiatePayment(buildRequest(1)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("successfully");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows re-initiation when existing payment is PENDING (retry flow)")
        void pendingExistingPayment_allowsRetryNotDuplicate() {
            when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(pendingPayment));

            // Razorpay will fail in test env — we only care it doesn't throw DuplicateResourceException
            try {
                paymentService.initiatePayment(buildRequest(1));
            } catch (DuplicateResourceException e) {
                throw new AssertionError("Should not throw DuplicateResourceException for PENDING", e);
            } catch (BadRequestException e) {
                // Razorpay connection failure — expected in unit test
                assertThat(e.getMessage()).containsIgnoringCase("razorpay");
            }
        }

        @Test
        @DisplayName("allows re-initiation when existing payment is FAILED (retry flow)")
        void failedExistingPayment_allowsRetryNotDuplicate() {
            when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(failedPayment));

            try {
                paymentService.initiatePayment(buildRequest(1));
            } catch (DuplicateResourceException e) {
                throw new AssertionError("Should not throw DuplicateResourceException for FAILED", e);
            } catch (BadRequestException e) {
                assertThat(e.getMessage()).containsIgnoringCase("razorpay");
            }
        }

        @Test
        @DisplayName("PENDING_PAYMENT appointment status is allowed for payment initiation")
        void pendingPaymentStatus_allowed() {
            when(appointmentClient.getById(3)).thenReturn(pendingPaymentAppointment);
            when(paymentRepository.findByAppointmentId(3)).thenReturn(Optional.empty());

            try {
                paymentService.initiatePayment(buildRequest(3));
            } catch (BadRequestException e) {
                // Only Razorpay failures are acceptable here
                assertThat(e.getMessage()).doesNotContain("PENDING_PAYMENT");
            }
        }

        @Test
        @DisplayName("no existing payment — proceeds to gateway call")
        void noExistingPayment_proceedsToGateway() {
            when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.empty());

            try {
                paymentService.initiatePayment(buildRequest(1));
            } catch (BadRequestException e) {
                assertThat(e.getMessage()).containsIgnoringCase("razorpay");
            }
        }

        @Test
        @DisplayName("creates a pending payment when Razorpay order creation succeeds")
        void successfulGatewayCall_savesPendingPayment() throws Exception {
            PaymentRequest request = buildRequest(1);
            OrderClient orderClient = org.mockito.Mockito.mock(OrderClient.class);

            when(appointmentClient.getById(1)).thenReturn(scheduledAppointment);
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.empty());
            when(orderClient.create(any(JSONObject.class))).thenReturn(buildOrder("order_live_123"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment saved = invocation.getArgument(0);
                saved.setPaymentId(501);
                saved.setCreatedAt(LocalDateTime.of(2026, 1, 2, 10, 15, 0));
                return saved;
            });

            try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(
                    RazorpayClient.class,
                    (mock, context) -> mock.orders = orderClient)) {
                PaymentResponse response = paymentService.initiatePayment(request);

                assertThat(mockedClient.constructed()).hasSize(1);
                assertThat(response.getPaymentId()).isEqualTo(501);
                assertThat(response.getStatus()).isEqualTo("PENDING");
                assertThat(response.getRazorpayOrderId()).isEqualTo("order_live_123");
                assertThat(response.getRazorpayPaymentId()).isNull();
                assertThat(response.getMessage()).contains("Order created");

                verify(orderClient).create(argThat(orderRequest ->
                        orderRequest.optInt("amount") == 50000
                                && "INR".equals(orderRequest.optString("currency"))
                                && "appt_1".equals(orderRequest.optString("receipt"))
                                && orderRequest.optInt("payment_capture") == 1));
                verify(paymentRepository).save(argThat(payment ->
                        payment.getAppointmentId() == 1
                                && payment.getPatientId() == 2
                                && payment.getAmount() == 500.0
                                && "PENDING".equals(payment.getStatus())
                                && "order_live_123".equals(payment.getRazorpayOrderId())
                                && payment.getRazorpayPaymentId() == null
                                && payment.getNotes().contains("CASH")));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // verifyPayment()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("verifyPayment()")
    class VerifyPaymentTests {

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown orderId")
        void unknownOrderId_throws() {
            when(paymentRepository.findByRazorpayOrderId("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.verifyPayment("UNKNOWN", "PAY_1", "sig"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws BadRequestException when payment already SUCCESS")
        void alreadySuccess_throws() {
            when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_1"))
                    .thenReturn(Optional.of(successPayment));

            assertThatThrownBy(() ->
                    paymentService.verifyPayment("MOCK_ORDER_1", "PAY_1", "sig"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already verified");
        }

        @Test
        @DisplayName("invalid signature — marks payment FAILED and throws BadRequestException")
        void invalidSignature_marksFailedThrows() {
            Payment payment = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("PENDING").razorpayOrderId("order_real_123")
                    .createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByRazorpayOrderId("order_real_123"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            assertThatThrownBy(() ->
                    paymentService.verifyPayment("order_real_123", "pay_abc", "wrong_sig"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid signature");

            verify(paymentRepository).save(argThat(p -> "FAILED".equals(p.getStatus())));
        }

        @Test
        @DisplayName("MOCK_ orderId bypasses signature check — marks SUCCESS, updates appointment")
        void mockOrderId_bypassesSignature_marksSuccess() {
            Payment payment = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("PENDING").razorpayOrderId("MOCK_ORDER_10")
                    .createdAt(LocalDateTime.now()).build();

            Payment saved = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("SUCCESS").razorpayOrderId("MOCK_ORDER_10")
                    .razorpayPaymentId("MOCK_PAY_XYZ").createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_10"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
            doNothing().when(appointmentClient).updateStatus(anyInt(), any());

            PaymentResponse response = paymentService.verifyPayment(
                    "MOCK_ORDER_10", "MOCK_PAY_XYZ", null);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            verify(appointmentClient).updateStatus(10, "SCHEDULED");
        }

        @Test
        @DisplayName("appointment update failure is non-fatal — payment still marked SUCCESS")
        void appointmentUpdateFails_paymentStillSuccess() {
            Payment payment = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("PENDING").razorpayOrderId("MOCK_ORDER_10")
                    .createdAt(LocalDateTime.now()).build();

            Payment saved = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("SUCCESS").razorpayOrderId("MOCK_ORDER_10")
                    .razorpayPaymentId("MOCK_PAY_XYZ").createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_10"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
            doThrow(new RuntimeException("Feign timeout"))
                    .when(appointmentClient).updateStatus(anyInt(), any());

            // Must NOT throw
            PaymentResponse response = paymentService.verifyPayment(
                    "MOCK_ORDER_10", "MOCK_PAY_XYZ", null);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("verifyPayment sets razorpayPaymentId and signature on saved payment")
        void verifyPayment_setsPaymentIdAndSignature() {
            Payment payment = Payment.builder()
                    .paymentId(200).appointmentId(10).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("PENDING").razorpayOrderId("MOCK_ORDER_10")
                    .createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByRazorpayOrderId("MOCK_ORDER_10"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(appointmentClient).updateStatus(anyInt(), any());

            paymentService.verifyPayment("MOCK_ORDER_10", "MOCK_PAY_XYZ", "some_sig");

            verify(paymentRepository).save(argThat(p ->
                    "MOCK_PAY_XYZ".equals(p.getRazorpayPaymentId()) &&
                    "some_sig".equals(p.getRazorpaySignature()) &&
                    "SUCCESS".equals(p.getStatus())));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getPaymentByAppointment()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPaymentByAppointment()")
    class GetPaymentByAppointmentTests {

        @Test
        @DisplayName("returns PaymentResponse when payment found")
        void found_returnsResponse() {
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(successPayment));

            PaymentResponse response = paymentService.getPaymentByAppointment(1);

            assertThat(response).isNotNull();
            assertThat(response.getAppointmentId()).isEqualTo(1);
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getAmount()).isEqualTo(500.0);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no payment for appointmentId")
        void notFound_throws() {
            when(paymentRepository.findByAppointmentId(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByAppointment(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("response includes correct currency and paymentMethod")
        void found_fieldsMapped() {
            when(paymentRepository.findByAppointmentId(1)).thenReturn(Optional.of(successPayment));

            PaymentResponse response = paymentService.getPaymentByAppointment(1);

            assertThat(response.getCurrency()).isEqualTo("INR");
            assertThat(response.getPaymentMethod()).isEqualTo("UPI");
            assertThat(response.getRazorpayOrderId()).isEqualTo("MOCK_ORDER_1");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getPaymentById()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPaymentById()")
    class GetPaymentByIdTests {

        @Test
        @DisplayName("returns PaymentResponse when payment found")
        void found_returnsResponse() {
            when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));

            PaymentResponse response = paymentService.getPaymentById(101);

            assertThat(response).isNotNull();
            assertThat(response.getPaymentId()).isEqualTo(101);
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when payment not found")
        void notFound_throws() {
            when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("transactionTime is non-null even when createdAt is null")
        void nullCreatedAt_transactionTimeStillPresent() {
            Payment paymentNullDate = Payment.builder()
                    .paymentId(200).appointmentId(1).patientId(2)
                    .amount(100.0).currency("INR").paymentMethod("UPI")
                    .status("PENDING").razorpayOrderId("MOCK_1")
                    .createdAt(null).build();

            when(paymentRepository.findByPaymentId(200)).thenReturn(Optional.of(paymentNullDate));

            PaymentResponse response = paymentService.getPaymentById(200);

            assertThat(response.getTransactionTime()).isNotNull().isNotBlank();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getPaymentsByPatient()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPaymentsByPatient()")
    class GetPaymentsByPatientTests {

        @Test
        @DisplayName("returns all payments for given patientId")
        void returnsList() {
            when(paymentRepository.findByPatientId(2))
                    .thenReturn(List.of(successPayment, pendingPayment));

            List<Payment> result = paymentService.getPaymentsByPatient(2);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> p.getPatientId() == 2);
        }

        @Test
        @DisplayName("returns empty list when no payments exist for patient")
        void noPayments_returnsEmptyList() {
            when(paymentRepository.findByPatientId(99)).thenReturn(List.of());

            List<Payment> result = paymentService.getPaymentsByPatient(99);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getPaymentsByProvider()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPaymentsByProvider()")
    class GetPaymentsByProviderTests {

        @Test
        @DisplayName("returns payments for given providerId")
        void returnsList() {
            when(paymentRepository.findPaymentsByProvider(10))
                    .thenReturn(List.of(successPayment));

            List<Payment> result = paymentService.getPaymentsByProvider(10);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no payments for provider")
        void noPayments_returnsEmptyList() {
            when(paymentRepository.findPaymentsByProvider(999)).thenReturn(List.of());

            List<Payment> result = paymentService.getPaymentsByProvider(999);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // initiateRefund()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("initiateRefund()")
    class InitiateRefundTests {

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown paymentId")
        void unknownPaymentId_throws() {
            when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.initiateRefund(999))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws BadRequestException when payment is PENDING")
        void pendingPayment_throws() {
            when(paymentRepository.findByPaymentId(100)).thenReturn(Optional.of(pendingPayment));

            assertThatThrownBy(() -> paymentService.initiateRefund(100))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("successful payments");
        }

        @Test
        @DisplayName("throws BadRequestException when payment is FAILED")
        void failedPayment_throws() {
            when(paymentRepository.findByPaymentId(102)).thenReturn(Optional.of(failedPayment));

            assertThatThrownBy(() -> paymentService.initiateRefund(102))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("successful payments");
        }

        @Test
        @DisplayName("throws BadRequestException when payment is already REFUNDED")
        void alreadyRefunded_throws() {
            when(paymentRepository.findByPaymentId(103)).thenReturn(Optional.of(refundedPayment));

            assertThatThrownBy(() -> paymentService.initiateRefund(103))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("successful payments");
        }

        @Test
        @DisplayName("SUCCESS payment with TXN_ paymentId — simulated refund succeeds")
        void successPaymentWithTxnId_refundSucceeds() {
            // TXN_ prefix → callRefundGateway returns true (simulated)
            Payment savedRefunded = Payment.builder()
                    .paymentId(101).appointmentId(1).patientId(2)
                    .amount(500.0).currency("INR").paymentMethod("UPI")
                    .status("REFUNDED").razorpayOrderId("MOCK_ORDER_1")
                    .razorpayPaymentId("TXN_MOCK_PAY_1")
                    .createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedRefunded);

            PaymentResponse response = paymentService.initiateRefund(101);

            assertThat(response.getStatus()).isEqualTo("REFUNDED");
            verify(paymentRepository).save(argThat(p -> "REFUNDED".equals(p.getStatus())));
        }

        @Test
        @DisplayName("SUCCESS payment with null razorpayPaymentId — simulated refund succeeds")
        void nullPaymentId_simulatedRefundSucceeds() {
            Payment successNullPayId = Payment.builder()
                    .paymentId(104).appointmentId(6).patientId(2)
                    .amount(200.0).currency("INR").paymentMethod("CARD")
                    .status("SUCCESS").razorpayOrderId("MOCK_ORDER_6")
                    .razorpayPaymentId(null).createdAt(LocalDateTime.now()).build();

            Payment savedRefunded = Payment.builder()
                    .paymentId(104).appointmentId(6).patientId(2)
                    .amount(200.0).currency("INR").paymentMethod("CARD")
                    .status("REFUNDED").razorpayOrderId("MOCK_ORDER_6")
                    .createdAt(LocalDateTime.now()).build();

            when(paymentRepository.findByPaymentId(104)).thenReturn(Optional.of(successNullPayId));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedRefunded);

            PaymentResponse response = paymentService.initiateRefund(104);

            assertThat(response.getStatus()).isEqualTo("REFUNDED");
        }

        @Test
        @DisplayName("SUCCESS payment with pay_ paymentId uses Razorpay refund flow")
        void realRazorpayPaymentId_refundSucceeds() throws Exception {
            Payment realSuccessPayment = Payment.builder()
                    .paymentId(105).appointmentId(7).patientId(2)
                    .amount(400.0).currency("INR").paymentMethod("UPI")
                    .status("SUCCESS").razorpayOrderId("order_live_7")
                    .razorpayPaymentId("pay_live_7").createdAt(LocalDateTime.now()).build();
            PaymentClient paymentClient = org.mockito.Mockito.mock(PaymentClient.class);

            when(paymentRepository.findByPaymentId(105)).thenReturn(Optional.of(realSuccessPayment));
            when(paymentClient.refund(any(String.class), any(JSONObject.class)))
                    .thenReturn(buildRefund("processed"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(
                    RazorpayClient.class,
                    (mock, context) -> mock.payments = paymentClient)) {
                PaymentResponse response = paymentService.initiateRefund(105);

                assertThat(mockedClient.constructed()).hasSize(1);
                assertThat(response.getStatus()).isEqualTo("REFUNDED");
                assertThat(response.getMessage()).contains("Refund initiated successfully");

                verify(paymentClient).refund(argThat("pay_live_7"::equals), any(JSONObject.class));
                verify(paymentRepository).save(argThat(payment ->
                        "REFUNDED".equals(payment.getStatus())
                                && payment.getNotes().contains("Refund initiated via Razorpay")));
            }
        }

        @Test
        @DisplayName("refund failure from Razorpay status is surfaced as BadRequestException")
        void realRazorpayPaymentId_failedRefundStatusThrows() throws Exception {
            Payment realSuccessPayment = Payment.builder()
                    .paymentId(106).appointmentId(8).patientId(2)
                    .amount(450.0).currency("INR").paymentMethod("UPI")
                    .status("SUCCESS").razorpayOrderId("order_live_8")
                    .razorpayPaymentId("pay_live_8").createdAt(LocalDateTime.now()).build();
            PaymentClient paymentClient = org.mockito.Mockito.mock(PaymentClient.class);

            when(paymentRepository.findByPaymentId(106)).thenReturn(Optional.of(realSuccessPayment));
            when(paymentClient.refund(any(String.class), any(JSONObject.class)))
                    .thenReturn(buildRefund("failed"));

            try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(
                    RazorpayClient.class,
                    (mock, context) -> mock.payments = paymentClient)) {
                assertThatThrownBy(() -> paymentService.initiateRefund(106))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("Refund failed");

                assertThat(mockedClient.constructed()).hasSize(1);
                verify(paymentRepository, never()).save(any(Payment.class));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getPaymentsByStatus()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPaymentsByStatus()")
    class GetPaymentsByStatusTests {

        @Test
        @DisplayName("returns list for valid status SUCCESS")
        void success_returnsList() {
            when(paymentRepository.findByStatus("SUCCESS")).thenReturn(List.of(successPayment));

            List<Payment> result = paymentService.getPaymentsByStatus("SUCCESS");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("returns list for valid status PENDING")
        void pending_returnsList() {
            when(paymentRepository.findByStatus("PENDING")).thenReturn(List.of(pendingPayment));

            assertThat(paymentService.getPaymentsByStatus("PENDING")).hasSize(1);
        }

        @Test
        @DisplayName("returns list for valid status FAILED")
        void failed_returnsList() {
            when(paymentRepository.findByStatus("FAILED")).thenReturn(List.of(failedPayment));

            assertThat(paymentService.getPaymentsByStatus("FAILED")).hasSize(1);
        }

        @Test
        @DisplayName("returns list for valid status REFUNDED")
        void refunded_returnsList() {
            when(paymentRepository.findByStatus("REFUNDED")).thenReturn(List.of(refundedPayment));

            assertThat(paymentService.getPaymentsByStatus("REFUNDED")).hasSize(1);
        }

        @Test
        @DisplayName("throws BadRequestException for invalid status")
        void invalidStatus_throws() {
            assertThatThrownBy(() -> paymentService.getPaymentsByStatus("UNKNOWN"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status");
        }

        @Test
        @DisplayName("throws BadRequestException for empty string status")
        void emptyStatus_throws() {
            assertThatThrownBy(() -> paymentService.getPaymentsByStatus(""))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("throws BadRequestException for null status")
        void nullStatus_throws() {
            assertThatThrownBy(() -> paymentService.getPaymentsByStatus(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getTotalRevenue()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getTotalRevenue()")
    class GetTotalRevenueTests {

        @Test
        @DisplayName("returns correct sum from repository")
        void returnsSum() {
            when(paymentRepository.calculateTotalRevenue()).thenReturn(15000.0);

            assertThat(paymentService.getTotalRevenue()).isEqualTo(15000.0);
        }

        @Test
        @DisplayName("returns 0.0 when repository returns null")
        void nullResult_returnsZero() {
            when(paymentRepository.calculateTotalRevenue()).thenReturn(null);

            assertThat(paymentService.getTotalRevenue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns 0.0 when revenue is explicitly zero")
        void zeroRevenue_returnsZero() {
            when(paymentRepository.calculateTotalRevenue()).thenReturn(0.0);

            assertThat(paymentService.getTotalRevenue()).isEqualTo(0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getRevenueByProvider()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getRevenueByProvider()")
    class GetRevenueByProviderTests {

        @Test
        @DisplayName("returns revenue sum for given providerId")
        void returnsRevenue() {
            when(paymentRepository.calculateRevenueByProvider(10)).thenReturn(8500.0);

            assertThat(paymentService.getRevenueByProvider(10)).isEqualTo(8500.0);
        }

        @Test
        @DisplayName("returns 0.0 when repository returns null")
        void nullResult_returnsZero() {
            when(paymentRepository.calculateRevenueByProvider(99)).thenReturn(null);

            assertThat(paymentService.getRevenueByProvider(99)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns 0.0 when revenue is explicitly zero")
        void zeroRevenue_returnsZero() {
            when(paymentRepository.calculateRevenueByProvider(10)).thenReturn(0.0);

            assertThat(paymentService.getRevenueByProvider(10)).isEqualTo(0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // updatePaymentStatus()
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updatePaymentStatus()")
    class UpdatePaymentStatusTests {

        @Test
        @DisplayName("updates status to REFUNDED successfully")
        void updatesToRefunded() {
            when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(successPayment);

            paymentService.updatePaymentStatus(101, "REFUNDED");

            verify(paymentRepository).save(argThat(p -> "REFUNDED".equals(p.getStatus())));
        }

        @Test
        @DisplayName("updates status to SUCCESS successfully")
        void updatesToSuccess() {
            when(paymentRepository.findByPaymentId(100)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

            paymentService.updatePaymentStatus(100, "SUCCESS");

            verify(paymentRepository).save(argThat(p -> "SUCCESS".equals(p.getStatus())));
        }

        @Test
        @DisplayName("updates status to PENDING successfully")
        void updatesToPending() {
            when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(successPayment);

            paymentService.updatePaymentStatus(101, "PENDING");

            verify(paymentRepository).save(argThat(p -> "PENDING".equals(p.getStatus())));
        }

        @Test
        @DisplayName("updates status to FAILED successfully")
        void updatesToFailed() {
            when(paymentRepository.findByPaymentId(100)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

            paymentService.updatePaymentStatus(100, "FAILED");

            verify(paymentRepository).save(argThat(p -> "FAILED".equals(p.getStatus())));
        }

        @Test
        @DisplayName("throws BadRequestException for invalid status — repository never called")
        void invalidStatus_throwsBeforeRepo() {
            assertThatThrownBy(() -> paymentService.updatePaymentStatus(101, "INVALID_XYZ"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status");

            verify(paymentRepository, never()).findByPaymentId(anyInt());
        }

        @Test
        @DisplayName("throws BadRequestException for null status before repository lookup")
        void nullStatus_throwsBeforeRepo() {
            assertThatThrownBy(() -> paymentService.updatePaymentStatus(101, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status");

            verify(paymentRepository, never()).findByPaymentId(anyInt());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when payment not found")
        void paymentNotFound_throws() {
            when(paymentRepository.findByPaymentId(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.updatePaymentStatus(999, "REFUNDED"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("sets notes mentioning new status on update")
        void setsNotesWithNewStatus() {
            when(paymentRepository.findByPaymentId(101)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(successPayment);

            paymentService.updatePaymentStatus(101, "REFUNDED");

            verify(paymentRepository).save(argThat(p ->
                    p.getNotes() != null && p.getNotes().contains("REFUNDED")));
        }
    }
}
