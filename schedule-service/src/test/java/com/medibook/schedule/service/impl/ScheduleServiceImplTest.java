package com.medibook.schedule.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.schedule.dto.request.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.exception.BadRequestException;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.repository.SlotRepository;
import com.medibook.schedule.support.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void addSlotSavesValidatedSlot() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("weekly");
        when(slotRepository.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilitySlot saved = scheduleService.addSlot(request);

        assertThat(saved.getProviderId()).isEqualTo(request.getProviderId());
        assertThat(saved.getDate()).isEqualTo(request.getDate());
        assertThat(saved.getStartTime()).isEqualTo(request.getStartTime());
        assertThat(saved.getEndTime()).isEqualTo(request.getEndTime());
        assertThat(saved.getDurationMinutes()).isEqualTo(request.getDurationMinutes());
        assertThat(saved.getRecurrence()).isEqualTo("WEEKLY");
        assertThat(saved.isBooked()).isFalse();
        assertThat(saved.isBlocked()).isFalse();
        assertThat(request.getRecurrence()).isEqualTo("WEEKLY");
        verify(slotRepository).save(any(AvailabilitySlot.class));
    }

    @Test
    void addSlotRejectsNullRequest() {
        assertThatThrownBy(() -> scheduleService.addSlot(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slot request cannot be null.");
    }

    @Test
    void addSlotRejectsPastDate() {
        SlotRequest request = TestDataFactory.validRequest(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> scheduleService.addSlot(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot create slot in the past. Please select a future date.");
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    void addSlotRejectsInvalidTimeRange() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setEndTime(request.getStartTime());

        assertThatThrownBy(() -> scheduleService.addSlot(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("End time must be after start time.");
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    void addBulkSlotsRejectsEmptyInput() {
        assertThatThrownBy(() -> scheduleService.addBulkSlots(List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slot list cannot be empty.");
    }

    @Test
    void addBulkSlotsSavesEachSlot() {
        SlotRequest first = TestDataFactory.validRequest();
        SlotRequest second = TestDataFactory.validRequest(LocalDate.now().plusDays(2));
        when(slotRepository.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AvailabilitySlot> savedSlots =
                scheduleService.addBulkSlots(List.of(first, second));

        assertThat(savedSlots).hasSize(2);
        assertThat(savedSlots).extracting(AvailabilitySlot::getDate)
                .containsExactly(first.getDate(), second.getDate());
        verify(slotRepository, times(2)).save(any(AvailabilitySlot.class));
    }

    @Test
    void generateRecurringSlotsRequiresRecurrenceEndDate() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("DAILY");

        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Recurrence end date is required for recurring slots.");
    }

    @Test
    void generateRecurringSlotsRejectsEarlierEndDate() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("DAILY");
        request.setRecurrenceEndDate(request.getDate().minusDays(1));

        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Recurrence end date must be after start date.");
    }

    @Test
    void generateRecurringSlotsRejectsUnsupportedRecurrence() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("MONTHLY");
        request.setRecurrenceEndDate(request.getDate().plusDays(3));

        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Recurrence pattern must be NONE, DAILY, or WEEKLY.");
    }

    @Test
    void generateRecurringSlotsCreatesDailySeries() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("daily");
        request.setRecurrenceEndDate(request.getDate().plusDays(2));
        AtomicInteger counter = new AtomicInteger(100);
        when(slotRepository.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> {
                    AvailabilitySlot slot = invocation.getArgument(0);
                    slot.setSlotId(counter.getAndIncrement());
                    return slot;
                });

        List<AvailabilitySlot> slots =
                scheduleService.generateRecurringSlots(request);

        assertThat(slots).hasSize(3);
        assertThat(slots).extracting(AvailabilitySlot::getDate)
                .containsExactly(
                        request.getDate(),
                        request.getDate().plusDays(1),
                        request.getDate().plusDays(2)
                );
        assertThat(slots).extracting(AvailabilitySlot::getRecurrence)
                .containsOnly("DAILY");
        verify(slotRepository, times(3)).save(any(AvailabilitySlot.class));
    }

    @Test
    void generateRecurringSlotsCreatesWeeklySeries() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setRecurrence("weekly");
        request.setRecurrenceEndDate(request.getDate().plusWeeks(2));
        when(slotRepository.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AvailabilitySlot> slots =
                scheduleService.generateRecurringSlots(request);

        assertThat(slots).hasSize(3);
        assertThat(slots).extracting(AvailabilitySlot::getDate)
                .containsExactly(
                        request.getDate(),
                        request.getDate().plusWeeks(1),
                        request.getDate().plusWeeks(2)
                );
        assertThat(request.getRecurrence()).isEqualTo("WEEKLY");
        verify(slotRepository, times(3)).save(any(AvailabilitySlot.class));
    }

    @Test
    void getSlotsByProviderReturnsRepositoryResults() {
        List<AvailabilitySlot> expected = List.of(TestDataFactory.validSlot());
        when(slotRepository.findByProviderId(10)).thenReturn(expected);

        List<AvailabilitySlot> result = scheduleService.getSlotsByProvider(10);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getAvailableSlotsRequiresDate() {
        assertThatThrownBy(() -> scheduleService.getAvailableSlots(10, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Date cannot be null.");
    }

    @Test
    void getAvailableSlotsReturnsRepositoryResults() {
        LocalDate date = LocalDate.now().plusDays(1);
        List<AvailabilitySlot> expected = List.of(TestDataFactory.validSlot(7, date));
        when(slotRepository.findAvailableByProviderAndDate(10, date))
                .thenReturn(expected);

        List<AvailabilitySlot> result =
                scheduleService.getAvailableSlots(10, date);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getSlotByIdThrowsWhenMissing() {
        when(slotRepository.findBySlotId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getSlotById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Slot not found with id: 99");
    }

    @Test
    void bookSlotRejectsAlreadyBookedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This slot is already booked. Please choose another slot.");
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    void bookSlotRejectsBlockedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBlocked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.bookSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This slot is blocked by the doctor and cannot be booked.");
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    void bookSlotMarksSlotAsBooked() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.bookSlot(1);

        assertThat(slot.isBooked()).isTrue();
        verify(slotRepository).save(slot);
    }

    @Test
    void releaseSlotSkipsUnbookedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.releaseSlot(1);

        assertThat(slot.isBooked()).isFalse();
        verify(slotRepository, never()).save(any(AvailabilitySlot.class));
    }

    @Test
    void releaseSlotClearsBookedFlag() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.releaseSlot(1);

        assertThat(slot.isBooked()).isFalse();
        verify(slotRepository).save(slot);
    }

    @Test
    void blockSlotRejectsBookedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.blockSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot block a slot that is already booked by a patient.");
    }

    @Test
    void blockSlotRejectsAlreadyBlockedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBlocked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.blockSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slot is already blocked.");
    }

    @Test
    void blockSlotMarksSlotAsBlocked() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.blockSlot(1);

        assertThat(slot.isBlocked()).isTrue();
        verify(slotRepository).save(slot);
    }

    @Test
    void unblockSlotRejectsVisibleSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.unblockSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Slot is not blocked.");
    }

    @Test
    void unblockSlotMarksSlotAsVisibleAgain() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBlocked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.unblockSlot(1);

        assertThat(slot.isBlocked()).isFalse();
        verify(slotRepository).save(slot);
    }

    @Test
    void updateSlotRejectsBookedSlot() {
        AvailabilitySlot existing = TestDataFactory.validSlot();
        existing.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> scheduleService.updateSlot(1, TestDataFactory.validRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot update a slot that is already booked by a patient.");
    }

    @Test
    void updateSlotRejectsPastDate() {
        AvailabilitySlot existing = TestDataFactory.validSlot();
        SlotRequest request = TestDataFactory.validRequest(LocalDate.now().minusDays(1));
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> scheduleService.updateSlot(1, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot update slot to a past date.");
    }

    @Test
    void updateSlotSavesNewValues() {
        AvailabilitySlot existing = TestDataFactory.validSlot();
        SlotRequest request = TestDataFactory.validRequest(LocalDate.now().plusDays(3));
        request.setStartTime(LocalTime.of(11, 0));
        request.setEndTime(LocalTime.of(11, 45));
        request.setDurationMinutes(45);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(existing));
        when(slotRepository.save(existing)).thenReturn(existing);

        AvailabilitySlot updated = scheduleService.updateSlot(1, request);

        assertThat(updated.getDate()).isEqualTo(request.getDate());
        assertThat(updated.getStartTime()).isEqualTo(request.getStartTime());
        assertThat(updated.getEndTime()).isEqualTo(request.getEndTime());
        assertThat(updated.getDurationMinutes()).isEqualTo(request.getDurationMinutes());
        verify(slotRepository).save(existing);
    }

    @Test
    void deleteSlotRejectsBookedSlot() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        slot.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.deleteSlot(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete a slot that is already booked by a patient. Please cancel the appointment first.");
    }

    @Test
    void deleteSlotDeletesById() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        scheduleService.deleteSlot(1);

        verify(slotRepository).deleteBySlotId(1);
    }

    @Test
    void deleteExpiredSlotsDeletesAllExpiredRecords() {
        AvailabilitySlot first = TestDataFactory.validSlot(11, LocalDate.now().minusDays(2));
        AvailabilitySlot second = TestDataFactory.validSlot(12, LocalDate.now().minusDays(1));
        when(slotRepository.findExpiredSlots(any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        scheduleService.deleteExpiredSlots();

        verify(slotRepository).deleteBySlotId(11);
        verify(slotRepository).deleteBySlotId(12);
    }
}
