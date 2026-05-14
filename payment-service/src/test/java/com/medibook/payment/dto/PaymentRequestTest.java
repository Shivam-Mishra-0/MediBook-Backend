package com.medibook.payment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.medibook.payment.dto.request.AppointmentDto;
import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

class PaymentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ── PaymentRequest ─────────────────────────────────────────────────────
    @Test
    @DisplayName("valid PaymentRequest has no constraint violations")
    void validRequest_noViolations() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(500.0);
        req.setPaymentMethod("UPI");
        req.setCurrency("INR");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PaymentRequest with amount 0 fails @Min(1) validation")
    void zeroAmount_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(0.0);
        req.setPaymentMethod("UPI");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("PaymentRequest with appointmentId 0 fails @Positive validation")
    void zeroAppointmentId_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(0);
        req.setPatientId(2);
        req.setAmount(100.0);
        req.setPaymentMethod("UPI");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("appointmentId"));
    }

    @Test
    @DisplayName("PaymentRequest with patientId 0 fails @Positive validation")
    void zeroPatientId_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(0);
        req.setAmount(100.0);
        req.setPaymentMethod("UPI");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("patientId"));
    }

    @Test
    @DisplayName("PaymentRequest with negative amount fails @Min(1) validation")
    void negativeAmount_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(-100.0);
        req.setPaymentMethod("UPI");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("PaymentRequest with blank paymentMethod fails @NotBlank validation")
    void blankPaymentMethod_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(100.0);
        req.setPaymentMethod("  ");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod"));
    }

    @Test
    @DisplayName("PaymentRequest with unsupported paymentMethod fails pattern validation")
    void invalidPaymentMethod_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(100.0);
        req.setPaymentMethod("CHEQUE");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod"));
    }

    @Test
    @DisplayName("PaymentRequest default currency is INR")
    void defaultCurrency_isINR() {
        PaymentRequest req = new PaymentRequest();
        assertThat(req.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("PaymentRequest with blank currency fails validation")
    void blankCurrency_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(100.0);
        req.setPaymentMethod("UPI");
        req.setCurrency(" ");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    @DisplayName("PaymentRequest with lowercase currency fails pattern validation")
    void lowercaseCurrency_fails() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(1);
        req.setPatientId(2);
        req.setAmount(100.0);
        req.setPaymentMethod("UPI");
        req.setCurrency("inr");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    @DisplayName("PaymentRequest getters/setters work via @Data")
    void gettersSetters() {
        PaymentRequest req = new PaymentRequest();
        req.setAppointmentId(5);
        req.setPatientId(10);
        req.setAmount(250.0);
        req.setPaymentMethod("CARD");
        req.setCurrency("USD");

        assertThat(req.getAppointmentId()).isEqualTo(5);
        assertThat(req.getPatientId()).isEqualTo(10);
        assertThat(req.getAmount()).isEqualTo(250.0);
        assertThat(req.getPaymentMethod()).isEqualTo("CARD");
        assertThat(req.getCurrency()).isEqualTo("USD");
    }

    // ── AppointmentDto ─────────────────────────────────────────────────────
    @Test
    @DisplayName("AppointmentDto getters/setters work via @Data")
    void appointmentDto_gettersSetters() {
        AppointmentDto dto = new AppointmentDto();
        dto.setAppointmentId(1);
        dto.setPatientId(2);
        dto.setProviderId(3);
        dto.setStatus("SCHEDULED");

        assertThat(dto.getAppointmentId()).isEqualTo(1);
        assertThat(dto.getPatientId()).isEqualTo(2);
        assertThat(dto.getProviderId()).isEqualTo(3);
        assertThat(dto.getStatus()).isEqualTo("SCHEDULED");
    }

    // ── PaymentResponse ────────────────────────────────────────────────────
    @Test
    @DisplayName("PaymentResponse builder sets all fields correctly")
    void paymentResponse_builder() {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId(1)
                .appointmentId(10)
                .status("SUCCESS")
                .amount(500.0)
                .currency("INR")
                .paymentMethod("UPI")
                .razorpayOrderId("order_abc")
                .razorpayPaymentId("pay_xyz")
                .message("Payment successful.")
                .transactionTime("2026-01-01 10:00:00")
                .build();

        assertThat(response.getPaymentId()).isEqualTo(1);
        assertThat(response.getAppointmentId()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getAmount()).isEqualTo(500.0);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getPaymentMethod()).isEqualTo("UPI");
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_abc");
        assertThat(response.getRazorpayPaymentId()).isEqualTo("pay_xyz");
        assertThat(response.getMessage()).isEqualTo("Payment successful.");
        assertThat(response.getTransactionTime()).isEqualTo("2026-01-01 10:00:00");
    }

    @Test
    @DisplayName("PaymentResponse no-args constructor creates object with null fields")
    void paymentResponse_noArgsConstructor() {
        PaymentResponse response = new PaymentResponse();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getAmount()).isZero();
    }

    @Test
    @DisplayName("PaymentResponse all-args constructor sets all fields")
    void paymentResponse_allArgsConstructor() {
        PaymentResponse r = new PaymentResponse(1, 2, "PENDING", 100.0, "INR",
                "CARD", "order_1", "pay_1", "Payment initiated.", "2026-01-01 00:00:00");

        assertThat(r.getPaymentId()).isEqualTo(1);
        assertThat(r.getAppointmentId()).isEqualTo(2);
        assertThat(r.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("PaymentResponse setters work correctly")
    void paymentResponse_setters() {
        PaymentResponse r = new PaymentResponse();
        r.setStatus("REFUNDED");
        r.setAmount(300.0);
        r.setMessage("Refunded.");

        assertThat(r.getStatus()).isEqualTo("REFUNDED");
        assertThat(r.getAmount()).isEqualTo(300.0);
        assertThat(r.getMessage()).isEqualTo("Refunded.");
    }
}
