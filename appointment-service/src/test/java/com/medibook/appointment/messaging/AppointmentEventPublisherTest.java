package com.medibook.appointment.messaging;

import com.medibook.appointment.config.RabbitMQConfig;
import com.medibook.appointment.dto.AppointmentEventDto;
import com.medibook.appointment.entity.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentEventPublisher Tests")
class AppointmentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AppointmentEventPublisher publisher;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointment = new Appointment();
        appointment.setAppointmentId(1);
        appointment.setPatientId(10);
        appointment.setProviderId(5);
        appointment.setSlotId(20);
        appointment.setServiceType("Dental Checkup");
        appointment.setModeOfConsultation("IN_PERSON");
        appointment.setAppointmentDate(LocalDate.of(2026, 6, 15));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus("SCHEDULED");
    }

    @Test
    @DisplayName("publishBooked should send to correct exchange and routing key")
    void shouldPublishBookedEventToCorrectKey() {
        publisher.publishBooked(appointment);

        ArgumentCaptor<AppointmentEventDto> eventCaptor = ArgumentCaptor.forClass(AppointmentEventDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.KEY_BOOKED),
                eventCaptor.capture()
        );

        AppointmentEventDto event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("BOOKED");
        assertThat(event.getAppointmentId()).isEqualTo(1);
        assertThat(event.getPatientId()).isEqualTo(10);
        assertThat(event.getProviderId()).isEqualTo(5);
        assertThat(event.getServiceType()).isEqualTo("Dental Checkup");
        assertThat(event.getModeOfConsultation()).isEqualTo("IN_PERSON");
        assertThat(event.getAppointmentDate()).isEqualTo("2026-06-15");
        assertThat(event.getStartTime()).isEqualTo("10:00");
        assertThat(event.getMessage()).contains("2026-06-15");
    }

    @Test
    @DisplayName("publishCancelled should send to correct exchange and routing key")
    void shouldPublishCancelledEventToCorrectKey() {
        publisher.publishCancelled(appointment);

        ArgumentCaptor<AppointmentEventDto> eventCaptor = ArgumentCaptor.forClass(AppointmentEventDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.KEY_CANCELLED),
                eventCaptor.capture()
        );

        AppointmentEventDto event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("CANCELLED");
        assertThat(event.getAppointmentId()).isEqualTo(1);
        assertThat(event.getMessage()).contains("cancelled");
    }

    @Test
    @DisplayName("publishCompleted should send to correct exchange and routing key")
    void shouldPublishCompletedEventToCorrectKey() {
        publisher.publishCompleted(appointment);

        ArgumentCaptor<AppointmentEventDto> eventCaptor = ArgumentCaptor.forClass(AppointmentEventDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.KEY_COMPLETED),
                eventCaptor.capture()
        );

        AppointmentEventDto event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("COMPLETED");
        assertThat(event.getMessage()).contains("completed");
    }

    @Test
    @DisplayName("publishBooked event should include endTime")
    void shouldIncludeEndTimeInEvent() {
        publisher.publishBooked(appointment);

        ArgumentCaptor<AppointmentEventDto> captor = ArgumentCaptor.forClass(AppointmentEventDto.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());

        assertThat(captor.getValue().getEndTime()).isEqualTo("10:30");
    }

    @Test
    @DisplayName("Each publish method should call rabbitTemplate.convertAndSend exactly once")
    void shouldCallRabbitTemplateExactlyOnce() {
        publisher.publishBooked(appointment);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(AppointmentEventDto.class));

        reset(rabbitTemplate);
        publisher.publishCancelled(appointment);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(AppointmentEventDto.class));

        reset(rabbitTemplate);
        publisher.publishCompleted(appointment);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(AppointmentEventDto.class));
    }
}
