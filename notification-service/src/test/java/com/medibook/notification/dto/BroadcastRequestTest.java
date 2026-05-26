package com.medibook.notification.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BroadcastRequestTest {

    @Test
    void testSettersAndGetters() {
        BroadcastRequest request = new BroadcastRequest();
        request.setTitle("System Maintenance");
        request.setMessage("The system will be down for maintenance.");

        assertEquals("System Maintenance", request.getTitle());
        assertEquals("The system will be down for maintenance.", request.getMessage());
    }

    @Test
    void testDefaultValues() {
        BroadcastRequest request = new BroadcastRequest();
        assertNull(request.getTitle());
        assertNull(request.getMessage());
    }

    @Test
    void testEqualsAndHashCode() {
        BroadcastRequest r1 = new BroadcastRequest();
        r1.setTitle("Hello");
        r1.setMessage("World");

        BroadcastRequest r2 = new BroadcastRequest();
        r2.setTitle("Hello");
        r2.setMessage("World");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testNotEqualWhenDifferent() {
        BroadcastRequest r1 = new BroadcastRequest();
        r1.setTitle("Hello");
        r1.setMessage("World");

        BroadcastRequest r2 = new BroadcastRequest();
        r2.setTitle("Bye");
        r2.setMessage("Earth");

        assertNotEquals(r1, r2);
        assertNotEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testToString() {
        BroadcastRequest request = new BroadcastRequest();
        request.setTitle("Alert");
        request.setMessage("Critical update available.");

        String result = request.toString();
        assertTrue(result.contains("Alert"));
        assertTrue(result.contains("Critical update available."));
    }

    @Test
    void testCanEqualSameObject() {
        BroadcastRequest request = new BroadcastRequest();
        assertEquals(request, request);
    }

    @Test
    void testCanEqualDifferentType() {
        BroadcastRequest request = new BroadcastRequest();
        assertNotEquals(request, new Object());
        assertNotEquals(request, null);
    }

    @Test
    void testEmptyStringFields() {
        BroadcastRequest request = new BroadcastRequest();
        request.setTitle("");
        request.setMessage("");

        assertEquals("", request.getTitle());
        assertEquals("", request.getMessage());
    }
}
