package com.medibook.payment.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.payment.client.AppointmentClient;
import com.medibook.payment.dto.request.AppointmentDto;
import com.medibook.payment.dto.request.PaymentRequest;
import com.medibook.payment.dto.response.PaymentResponse;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.exception.BadRequestException;
import com.medibook.payment.exception.DuplicateResourceException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;

//import jakarta.annotation.PostConstruct;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("PENDING", "SUCCESS", "FAILED", "REFUNDED");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentClient appointmentClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String razorpayCurrency;

//     @PostConstruct
// public void checkConfig() {
//     System.out.println("Razorpay Key ID: " + 
//         (razorpayKeyId != null ? razorpayKeyId.substring(0, 8) + "..." : "NULL ❌"));
//     System.out.println("Razorpay Secret: " + 
//         (razorpayKeySecret != null ? "SET ✅" : "NULL ❌"));
// }

    // ── Inner class ────────────────────────────────────────────
    private static class GatewayResponse {
        String orderId; String paymentId; String status;
        GatewayResponse(String o, String p, String s) { orderId=o; paymentId=p; status=s; }
    }

    private GatewayResponse callGateway(PaymentRequest request) {
        return razorpayGateway(request);
    }

    private boolean callRefundGateway(String razorpayPaymentId, double amount) {
        if (razorpayPaymentId == null
                || razorpayPaymentId.startsWith("TXN_")
                || !razorpayPaymentId.startsWith("pay_")) {
            System.out.println("[Refund] Simulated payment detected (ID: " + razorpayPaymentId
                    + ") — processing as simulated refund.");
            return true;
        }
        return razorpayRefund(razorpayPaymentId, amount);
    }

    private GatewayResponse razorpayGateway(PaymentRequest request) {
        try {

            // okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.builder()
            //         .connectTimeout( 10, java.util.concurrent.TimeUnit.SECONDS)
            //         .readTimeout( 10, java.util.concurrent.Timeout.SECONDS)
            //         .writeTimeout( 10, java.util.concurrent.TimeUnit.SECONDS)
            //         .build();


            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(request.getAmount() * 100));
            orderRequest.put("currency", razorpayCurrency);
            orderRequest.put("receipt", "appt_" + request.getAppointmentId());
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            return new GatewayResponse(order.get("id"), null, "PENDING");


        } catch (RazorpayException e) {
            throw new BadRequestException("Razorpay order creation failed: " + e.getMessage());
        } catch (Exception e)   {
            throw new BadRequestException("Payment gateway timed out. Please retry. " + e.getMessage());
        }
    }

    private boolean razorpayRefund(String razorpayPaymentId, double amount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (int)(amount * 100));
            Refund refund = client.payments.refund(razorpayPaymentId, refundRequest);
            String refundStatus = refund.get("status");
            return refundStatus.equals("processed") || refundStatus.equals("initiated");
        } catch (RazorpayException e) {
            throw new BadRequestException("Razorpay refund failed: " + e.getMessage());
        }
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        if (orderId != null && orderId.startsWith("MOCK_")) return true;
        try {
            String message = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(razorpayKeySecret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            throw new BadRequestException("Signature verification failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        AppointmentDto appointment = appointmentClient.getById(request.getAppointmentId());

        // ✅ FIX: Allow "PENDING_PAYMENT" status in addition to "SCHEDULED"/"CONFIRMED".
        //
        // OLD (BUGGY):
        //   Only allowed SCHEDULED or CONFIRMED → threw exception for PENDING_PAYMENT
        //   → Razorpay never opened → slot auto-booked without payment
        //
        // NEW (CORRECT):
        //   bookAppointment() now sets status to PENDING_PAYMENT (awaiting payment).
        //   initiatePayment() must allow this status to proceed with Razorpay order creation.
        //   After payment success, verifyPayment() calls updateStatus("SCHEDULED").
        //
        // CANCELLED / COMPLETED appointments still correctly rejected.
        if (appointment.getStatus().equals("CANCELLED") || appointment.getStatus().equals("COMPLETED")) {
            throw new BadRequestException(
                "Payment cannot be made for a " + appointment.getStatus() + " appointment.");
        }

        // ✅ FIX: Allow re-initiation for PENDING_PAYMENT appointments.
        //
        // OLD (BUGGY):
        //   Blocked if any payment record already existed → patient could never retry
        //   if their first Razorpay attempt failed or was abandoned.
        //
        // NEW (CORRECT):
        //   If a payment record already exists AND it is already SUCCESS → block (no double payment).
        //   If it is PENDING or FAILED → allow re-initiation so patient can retry payment.
        paymentRepository.findByAppointmentId(request.getAppointmentId()).ifPresent(existing -> {
            if ("SUCCESS".equals(existing.getStatus())) {
                throw new DuplicateResourceException(
                    "Payment already completed successfully for appointment: "
                    + request.getAppointmentId());
            }
            // PENDING or FAILED → fall through and create a fresh Razorpay order
        });

        GatewayResponse gatewayResponse = callGateway(request);

        Payment payment = Payment.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(gatewayResponse.status)
                .razorpayOrderId(gatewayResponse.orderId)
                .razorpayPaymentId(gatewayResponse.paymentId)
                .notes("Payment initiated via " + request.getPaymentMethod())
                .build();

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Order created. Complete payment in popup.");
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", razorpayOrderId));

        if (payment.getStatus().equals("SUCCESS"))
            throw new BadRequestException("Payment is already verified and successful.");

        boolean isValidSignature = verifyRazorpaySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (!isValidSignature) {
            payment.setStatus("FAILED");
            payment.setNotes("Payment failed — invalid signature detected.");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed. Invalid signature.");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus("SUCCESS");
        payment.setNotes("Payment verified via Razorpay. Transaction: " + razorpayPaymentId);

        // ✅ Payment verified → NOW mark appointment SCHEDULED and book the slot.
        // This is the correct trigger point — slot is only locked after confirmed payment.
        try {
            appointmentClient.updateStatus(payment.getAppointmentId(), "SCHEDULED");
            System.out.println("✅ Appointment marked SCHEDULED after payment success. AppointmentId: "
                    + payment.getAppointmentId());
        } catch (Exception e) {
            // Non-fatal log — do not roll back payment if appointment update fails.
            // Admin can manually fix appointment status if needed.
            System.err.println("❌ Failed to update appointment status after payment: " + e.getMessage());
        }

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Payment successful. Appointment confirmed.");
    }

    @Override
    public PaymentResponse getPaymentByAppointment(int appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "appointmentId", appointmentId));
        return buildResponse(payment, "Payment details retrieved.");
    }

    @Override
    public PaymentResponse getPaymentById(int paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return buildResponse(payment, "Payment details retrieved.");
    }

    @Override
    public List<Payment> getPaymentsByPatient(int patientId) {
        return paymentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Payment> getPaymentsByProvider(int providerId) {
        return paymentRepository.findPaymentsByProvider(providerId);
    }

    @Override
    @Transactional
    public PaymentResponse initiateRefund(int paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.getStatus().equals("SUCCESS"))
            throw new BadRequestException("Refund can only be initiated for successful payments. Status: " + payment.getStatus());

        boolean refundSuccess = callRefundGateway(payment.getRazorpayPaymentId(), payment.getAmount());

        if (refundSuccess) {
            payment.setStatus("REFUNDED");
            payment.setNotes("Refund initiated via Razorpay. Amount will be credited in 5-7 business days.");
        } else {
            throw new BadRequestException("Refund failed. Please contact support.");
        }

        Payment saved = paymentRepository.save(payment);
        return buildResponse(saved, "Refund initiated successfully.");
    }

    @Override
    public List<Payment> getPaymentsByStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new BadRequestException("Invalid status. Allowed: PENDING, SUCCESS, FAILED, REFUNDED");
        }
        return paymentRepository.findByStatus(status);
    }

    @Override
    public double getTotalRevenue() {
        Double total = paymentRepository.calculateTotalRevenue();
        return total != null ? total : 0.0;
    }

    @Override
    public double getRevenueByProvider(int providerId) {
        Double total = paymentRepository.calculateRevenueByProvider(providerId);
        return total != null ? total : 0.0;
    }

    @Override
    public void updatePaymentStatus(int paymentId, String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new BadRequestException("Invalid status.");
        }
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        payment.setStatus(status);
        payment.setNotes("Status manually updated to: " + status);
        paymentRepository.save(payment);
    }

    private PaymentResponse buildResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .appointmentId(payment.getAppointmentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .message(message)
                .transactionTime(
                    payment.getCreatedAt() != null
                        ? payment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .build();
    }
}
