package com.medibook.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class PaymentEntityTest {

    @Test
    @DisplayName("Builder creates Payment with all fields set correctly")
    void builder_allFields() {
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .paymentId(1)
                .appointmentId(10)
                .patientId(5)
                .amount(750.0)
                .currency("INR")
                .paymentMethod("UPI")
                .status("SUCCESS")
                .razorpayOrderId("order_abc123")
                .razorpayPaymentId("pay_xyz789")
                .razorpaySignature("sig_hmac")
                .notes("Test payment")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(payment.getPaymentId()).isEqualTo(1);
        assertThat(payment.getAppointmentId()).isEqualTo(10);
        assertThat(payment.getPatientId()).isEqualTo(5);
        assertThat(payment.getAmount()).isEqualTo(750.0);
        assertThat(payment.getCurrency()).isEqualTo("INR");
        assertThat(payment.getPaymentMethod()).isEqualTo("UPI");
        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getRazorpayOrderId()).isEqualTo("order_abc123");
        assertThat(payment.getRazorpayPaymentId()).isEqualTo("pay_xyz789");
        assertThat(payment.getRazorpaySignature()).isEqualTo("sig_hmac");
        assertThat(payment.getNotes()).isEqualTo("Test payment");
        assertThat(payment.getCreatedAt()).isEqualTo(now);
        assertThat(payment.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("No-args constructor creates Payment with default values")
    void noArgsConstructor() {
        Payment payment = new Payment();
        assertThat(payment.getPaymentId()).isZero();
        assertThat(payment.getAmount()).isZero();
        assertThat(payment.getCurrency()).isNull();
    }

    @Test
    @DisplayName("All-args constructor sets all fields")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment(1, 2, 3, 100.0, "INR", "CARD",
                "PENDING", "order_1", "pay_1", "sig_1", now, now, "note");

        assertThat(payment.getPaymentId()).isEqualTo(1);
        assertThat(payment.getAppointmentId()).isEqualTo(2);
        assertThat(payment.getPatientId()).isEqualTo(3);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Setters mutate fields correctly")
    void setters() {
        Payment payment = new Payment();
        payment.setPaymentId(99);
        payment.setAppointmentId(10);
        payment.setPatientId(5);
        payment.setAmount(200.0);
        payment.setCurrency("INR");
        payment.setPaymentMethod("NETBANKING");
        payment.setStatus("REFUNDED");
        payment.setRazorpayOrderId("order_new");
        payment.setRazorpayPaymentId("pay_new");
        payment.setRazorpaySignature("sig_new");
        payment.setNotes("Updated notes");

        assertThat(payment.getPaymentId()).isEqualTo(99);
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        assertThat(payment.getAmount()).isEqualTo(200.0);
        assertThat(payment.getNotes()).isEqualTo("Updated notes");
    }

    @Test
    @DisplayName("prePersist sets createdAt and updatedAt to non-null")
    void prePersist_setsTimestamps() {
        Payment payment = new Payment();
        assertThat(payment.getCreatedAt()).isNull();

        payment.prePersist();

        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("preUpdate sets updatedAt to non-null")
    void preUpdate_setsUpdatedAt() {
        Payment payment = new Payment();
        payment.preUpdate();

        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("equals and hashCode are consistent for same content")
    void equalsHashCode() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 0, 0);
        Payment a = Payment.builder().paymentId(1).appointmentId(2).patientId(3)
                .amount(100.0).currency("INR").paymentMethod("UPI")
                .status("PENDING").createdAt(ts).updatedAt(ts).build();
        Payment b = Payment.builder().paymentId(1).appointmentId(2).patientId(3)
                .amount(100.0).currency("INR").paymentMethod("UPI")
                .status("PENDING").createdAt(ts).updatedAt(ts).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("toString is non-null and contains paymentId")
    void toStringIsNonNull() {
        Payment payment = Payment.builder().paymentId(42).appointmentId(1)
                .patientId(1).amount(100.0).currency("INR")
                .paymentMethod("UPI").status("PENDING").build();

        assertThat(payment.toString()).isNotNull().contains("42");
    }

    @Test
    @DisplayName("default status field is PENDING when using setter path")
    void defaultStatus_isPending() {
        Payment payment = new Payment();
        // default field initializer on the class sets status = "PENDING"
        assertThat(payment.getStatus()).isEqualTo("PENDING");
    }
}
