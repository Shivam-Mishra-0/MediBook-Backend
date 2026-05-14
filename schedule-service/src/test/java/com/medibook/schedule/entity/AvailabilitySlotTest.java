package com.medibook.schedule.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.medibook.schedule.support.TestDataFactory;

class AvailabilitySlotTest {

    @Test
    void prePersistSetsCreatedAtAndBuilderPopulatesFields() {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .slotId(5)
                .providerId(10)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .recurrence("NONE")
                .version(1)
                .build();

        slot.prePersist();

        assertThat(slot.getCreatedAt()).isNotNull();
        assertThat(slot.getSlotId()).isEqualTo(5);
        assertThat(slot.getProviderId()).isEqualTo(10);
        assertThat(slot.getDurationMinutes()).isEqualTo(30);
        assertThat(slot.isBooked()).isFalse();
        assertThat(slot.isBlocked()).isFalse();
        assertThat(slot.getRecurrence()).isEqualTo("NONE");
        assertThat(slot.getVersion()).isEqualTo(1);
    }

    @Test
    void dataMethodsWorkForAvailabilitySlot() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        AvailabilitySlot first = AvailabilitySlot.builder()
                .slotId(1)
                .providerId(10)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .isBooked(false)
                .isBlocked(false)
                .recurrence("NONE")
                .createdAt(createdAt)
                .version(2)
                .build();
        AvailabilitySlot second = TestDataFactory.validSlot(1, first.getDate());
        second.setCreatedAt(createdAt);
        second.setVersion(2);

        assertThat(second).isEqualTo(first);
        assertThat(second.hashCode()).isEqualTo(first.hashCode());
        assertThat(second.toString()).contains("slotId=1");

        second.setBlocked(true);
        second.setBooked(true);
        assertThat(second.isBlocked()).isTrue();
        assertThat(second.isBooked()).isTrue();
        assertThat(second).isNotEqualTo(first);
    }
}
