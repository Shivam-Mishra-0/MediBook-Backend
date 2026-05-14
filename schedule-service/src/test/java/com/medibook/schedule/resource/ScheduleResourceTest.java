package com.medibook.schedule.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medibook.schedule.dto.request.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.exception.BadRequestException;
import com.medibook.schedule.exception.GlobalExceptionHandler;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.service.ScheduleService;
import com.medibook.schedule.support.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class ScheduleResourceTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleResource scheduleResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(scheduleResource)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void addSlotReturnsCreatedResponse() {
        SlotRequest request = TestDataFactory.validRequest();
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(scheduleService.addSlot(request)).thenReturn(slot);

        ResponseEntity<AvailabilitySlot> response =
                scheduleResource.addSlot(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(slot);
    }

    @Test
    void addBulkSlotsReturnsCreatedResponse() {
        SlotRequest request = TestDataFactory.validRequest();
        List<AvailabilitySlot> slots = List.of(TestDataFactory.validSlot());
        when(scheduleService.addBulkSlots(List.of(request))).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response =
                scheduleResource.addBulkSlots(List.of(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(slots);
    }

    @Test
    void generateRecurringReturnsCreatedResponse() {
        SlotRequest request = TestDataFactory.validRequest();
        List<AvailabilitySlot> slots = List.of(TestDataFactory.validSlot());
        when(scheduleService.generateRecurringSlots(request)).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response =
                scheduleResource.generateRecurring(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(slots);
    }

    @Test
    void getByProviderReturnsOkResponse() {
        List<AvailabilitySlot> slots = List.of(TestDataFactory.validSlot());
        when(scheduleService.getSlotsByProvider(10)).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response =
                scheduleResource.getByProvider(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(slots);
    }

    @Test
    void getAvailableReturnsOkResponse() {
        LocalDate date = LocalDate.now().plusDays(1);
        List<AvailabilitySlot> slots = List.of(TestDataFactory.validSlot(3, date));
        when(scheduleService.getAvailableSlots(10, date)).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response =
                scheduleResource.getAvailable(10, date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(slots);
    }

    @Test
    void getByIdReturnsOkResponse() {
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(scheduleService.getSlotById(1)).thenReturn(slot);

        ResponseEntity<AvailabilitySlot> response =
                scheduleResource.getById(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(slot);
    }

    @Test
    void updateSlotReturnsOkResponse() {
        SlotRequest request = TestDataFactory.validRequest();
        AvailabilitySlot slot = TestDataFactory.validSlot();
        when(scheduleService.updateSlot(1, request)).thenReturn(slot);

        ResponseEntity<AvailabilitySlot> response =
                scheduleResource.updateSlot(1, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(slot);
    }

    @Test
    void blockSlotReturnsSuccessMessage() {
        ResponseEntity<?> response = scheduleResource.blockSlot(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message",
                "Slot blocked successfully. It is now invisible to patients."
        ));
        verify(scheduleService).blockSlot(1);
    }

    @Test
    void unblockSlotReturnsSuccessMessage() {
        ResponseEntity<?> response = scheduleResource.unblockSlot(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message",
                "Slot unblocked successfully. It is now visible and bookable by patients."
        ));
        verify(scheduleService).unblockSlot(1);
    }

    @Test
    void deleteSlotReturnsSuccessMessage() {
        ResponseEntity<?> response = scheduleResource.deleteSlot(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message",
                "Slot deleted successfully."
        ));
        verify(scheduleService).deleteSlot(1);
    }

    @Test
    void bookSlotReturnsSuccessMessage() {
        ResponseEntity<?> response = scheduleResource.bookSlot(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message",
                "Slot marked as booked."
        ));
        verify(scheduleService).bookSlot(1);
    }

    @Test
    void releaseSlotReturnsSuccessMessage() {
        ResponseEntity<?> response = scheduleResource.releaseSlot(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message",
                "Slot released."
        ));
        verify(scheduleService).releaseSlot(1);
    }

    @Test
    void addSlotReturnsValidationErrorsForInvalidPayload() throws Exception {
        SlotRequest request = TestDataFactory.validRequest();
        request.setProviderId(0);
        request.setDurationMinutes(0);
        request.setEndTime(request.getStartTime());
        request.setRecurrence("monthly");

        mockMvc.perform(post("/slots/add")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.providerId")
                        .value("Provider ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.durationMinutes")
                        .value("Duration must be at least 1 minute"))
                .andExpect(jsonPath("$.errors.timeRangeValid")
                        .value("End time must be after start time"))
                .andExpect(jsonPath("$.errors.recurrenceValid")
                        .value("Recurrence must be NONE, DAILY, or WEEKLY"));
    }

    @Test
    void getByIdMapsResourceNotFoundException() throws Exception {
        when(scheduleService.getSlotById(99))
                .thenThrow(new ResourceNotFoundException("Slot", "id", 99));

        mockMvc.perform(get("/slots/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Slot not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/slots/99"));
    }

    @Test
    void addBulkSlotsMapsBadRequestException() throws Exception {
        doThrow(new BadRequestException("Slot list cannot be empty."))
                .when(scheduleService).addBulkSlots(List.of());

        mockMvc.perform(post("/slots/bulk")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Slot list cannot be empty."));
    }
}
