package com.medibook.schedule.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.schedule.service.ScheduleService;

@ExtendWith(MockitoExtension.class)
class SlotExpirySchedulerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private SlotExpiryScheduler scheduler;

    @Test
    void purgeExpiredSlotsDelegatesToService() {
        scheduler.purgeExpiredSlots();

        verify(scheduleService).deleteExpiredSlots();
    }

    @Test
    void purgeExpiredSlotsSwallowsServiceExceptions() {
        doThrow(new IllegalStateException("boom"))
                .when(scheduleService).deleteExpiredSlots();

        assertThatCode(() -> scheduler.purgeExpiredSlots())
                .doesNotThrowAnyException();
    }
}
