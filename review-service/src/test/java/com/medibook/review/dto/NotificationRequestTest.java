package com.medibook.review.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestTest {

    @Test
    void testSettersAndGetters() {
        NotificationRequest request = new NotificationRequest();

        request.setRecipientId(101);
        request.setType("EMAIL");
        request.setTitle("Appointment Confirmed");
        request.setMessage("Your appointment has been confirmed.");
        request.setChannel("EMAIL");

        assertEquals(101, request.getRecipientId());
        assertEquals("EMAIL", request.getType());
        assertEquals("Appointment Confirmed", request.getTitle());
        assertEquals("Your appointment has been confirmed.", request.getMessage());
        assertEquals("EMAIL", request.getChannel());
    }

    @Test
    void testDefaultValues() {
        NotificationRequest request = new NotificationRequest();

        assertEquals(0, request.getRecipientId());
        assertNull(request.getType());
        assertNull(request.getTitle());
        assertNull(request.getMessage());
        assertNull(request.getChannel());
    }

    @Test
    void testEqualsAndHashCode() {
        NotificationRequest request1 = new NotificationRequest();
        request1.setRecipientId(1);
        request1.setType("SMS");
        request1.setTitle("Test Title");
        request1.setMessage("Test Message");
        request1.setChannel("SMS");

        NotificationRequest request2 = new NotificationRequest();
        request2.setRecipientId(1);
        request2.setType("SMS");
        request2.setTitle("Test Title");
        request2.setMessage("Test Message");
        request2.setChannel("SMS");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testNotEqualWhenDifferent() {
        NotificationRequest request1 = new NotificationRequest();
        request1.setRecipientId(1);
        request1.setType("SMS");

        NotificationRequest request2 = new NotificationRequest();
        request2.setRecipientId(2);
        request2.setType("EMAIL");

        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(42);
        request.setType("PUSH");
        request.setTitle("New Review");
        request.setMessage("You received a new review.");
        request.setChannel("PUSH");

        String result = request.toString();

        assertTrue(result.contains("42"));
        assertTrue(result.contains("PUSH"));
        assertTrue(result.contains("New Review"));
        assertTrue(result.contains("You received a new review."));
    }

    @Test
    void testCanEqualSameObject() {
        NotificationRequest request = new NotificationRequest();
        assertEquals(request, request);
    }

    @Test
    void testCanEqualDifferentType() {
        NotificationRequest request = new NotificationRequest();
        assertNotEquals(request, new Object());
        assertNotEquals(request, null);
    }

    @Test
    void testAllChannelTypes() {
        NotificationRequest emailRequest = new NotificationRequest();
        emailRequest.setChannel("EMAIL");
        assertEquals("EMAIL", emailRequest.getChannel());

        NotificationRequest smsRequest = new NotificationRequest();
        smsRequest.setChannel("SMS");
        assertEquals("SMS", smsRequest.getChannel());

        NotificationRequest pushRequest = new NotificationRequest();
        pushRequest.setChannel("PUSH");
        assertEquals("PUSH", pushRequest.getChannel());
    }

    @Test
    void testRecipientIdBoundaryValues() {
        NotificationRequest request = new NotificationRequest();

        request.setRecipientId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, request.getRecipientId());

        request.setRecipientId(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, request.getRecipientId());

        request.setRecipientId(0);
        assertEquals(0, request.getRecipientId());
    }

    @Test
    void testEmptyStringFields() {
        NotificationRequest request = new NotificationRequest();
        request.setType("");
        request.setTitle("");
        request.setMessage("");
        request.setChannel("");

        assertEquals("", request.getType());
        assertEquals("", request.getTitle());
        assertEquals("", request.getMessage());
        assertEquals("", request.getChannel());
    }
}
