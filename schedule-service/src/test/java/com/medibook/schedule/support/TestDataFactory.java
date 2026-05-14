package com.medibook.schedule.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.medibook.schedule.dto.request.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static SlotRequest validRequest() {
        return validRequest(LocalDate.now().plusDays(1));
    }

    public static SlotRequest validRequest(LocalDate date) {
        SlotRequest request = new SlotRequest();
        request.setProviderId(10);
        request.setDate(date);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(10, 30));
        request.setDurationMinutes(30);
        request.setRecurrence("NONE");
        return request;
    }

    public static AvailabilitySlot validSlot() {
        return validSlot(1, LocalDate.now().plusDays(1));
    }

    public static AvailabilitySlot validSlot(int slotId, LocalDate date) {
        return AvailabilitySlot.builder()
                .slotId(slotId)
                .providerId(10)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .recurrence("NONE")
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .version(0)
                .build();
    }
}
