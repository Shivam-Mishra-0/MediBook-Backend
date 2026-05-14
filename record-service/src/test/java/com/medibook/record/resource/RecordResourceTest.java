package com.medibook.record.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.record.config.SecurityConfig;
import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BadRequestException;
import com.medibook.record.exception.GlobalExceptionHandler;
import com.medibook.record.service.RecordService;

@WebMvcTest(RecordResource.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RecordResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecordService recordService;

    @Test
    void createRecordReturnsCreatedRecord() throws Exception {
        when(recordService.createRecord(any(RecordRequest.class))).thenReturn(createRecord());

        mockMvc.perform(post("/records/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId").value(1))
                .andExpect(jsonPath("$.diagnosis").value("Viral infection"));
    }

    @Test
    void createRecordRejectsInvalidPayload() throws Exception {
        RecordRequest request = createRequest();
        request.setAppointmentId(0);
        request.setPatientId(0);
        request.setProviderId(0);
        request.setDiagnosis(" ");
        request.setFollowUpDate(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/records/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.appointmentId").value("Appointment ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.patientId").value("Patient ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.providerId").value("Provider ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.diagnosis").value("Diagnosis is required"))
                .andExpect(jsonPath("$.errors.followUpDate").value("Follow up date cannot be in the past"));
    }

    @Test
    void getByAppointmentReturnsRecord() throws Exception {
        when(recordService.getRecordByAppointment(500)).thenReturn(createRecord());

        mockMvc.perform(get("/records/appointment/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(500));
    }

    @Test
    void getByPatientReturnsRecords() throws Exception {
        when(recordService.getRecordsByPatient(101)).thenReturn(List.of(createRecord()));

        mockMvc.perform(get("/records/patient/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(101));
    }

    @Test
    void getByProviderReturnsRecords() throws Exception {
        when(recordService.getRecordsByProvider(202)).thenReturn(List.of(createRecord()));

        mockMvc.perform(get("/records/provider/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(202));
    }

    @Test
    void getByIdReturnsRecord() throws Exception {
        when(recordService.getRecordById(1)).thenReturn(createRecord());

        mockMvc.perform(get("/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(1));
    }

    @Test
    void updateRecordReturnsUpdatedRecord() throws Exception {
        MedicalRecord updated = createRecord();
        updated.setDiagnosis("Updated diagnosis");

        when(recordService.updateRecord(anyInt(), any(RecordRequest.class))).thenReturn(updated);

        RecordRequest request = createRequest();
        request.setDiagnosis("Updated diagnosis");

        mockMvc.perform(put("/records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Updated diagnosis"));
    }

    @Test
    void deleteRecordReturnsSuccessMessage() throws Exception {
        doNothing().when(recordService).deleteRecord(1);

        mockMvc.perform(delete("/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Medical record deleted successfully."));
    }

    @Test
    void attachDocumentReturnsUrlPayload() throws Exception {
        doNothing().when(recordService).attachDocument(1, "https://files.test/report.pdf");

        mockMvc.perform(put("/records/1/attach").param("url", "https://files.test/report.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document attached successfully."))
                .andExpect(jsonPath("$.attachmentUrl").value("https://files.test/report.pdf"));
    }

    @Test
    void getUpcomingFollowUpsReturnsRecords() throws Exception {
        when(recordService.getUpcomingFollowUps(101)).thenReturn(List.of(createRecord()));

        mockMvc.perform(get("/records/patient/101/followups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId").value(1));
    }

    @Test
    void getTodaysFollowUpsReturnsRecords() throws Exception {
        when(recordService.getFollowUpRecords(any(LocalDate.class))).thenReturn(List.of(createRecord()));

        mockMvc.perform(get("/records/followups/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followUpDate").exists());
    }

    @Test
    void getRecordCountReturnsSummaryPayload() throws Exception {
        when(recordService.getRecordCount(101)).thenReturn(4);

        mockMvc.perform(get("/records/patient/101/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(101))
                .andExpect(jsonPath("$.totalRecords").value(4));
    }

    @Test
    void controllerAdviceHandlesBusinessException() throws Exception {
        when(recordService.getFollowUpRecords(any(LocalDate.class)))
                .thenThrow(new BadRequestException("Date cannot be null."));

        mockMvc.perform(get("/records/followups/today"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Date cannot be null."));
    }

    @Test
    void securityConfigurationAllowsPostWithoutCsrfToken() throws Exception {
        when(recordService.createRecord(any(RecordRequest.class))).thenReturn(createRecord());

        mockMvc.perform(post("/records/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
    }

    private RecordRequest createRequest() {
        RecordRequest request = new RecordRequest();
        request.setAppointmentId(500);
        request.setPatientId(101);
        request.setProviderId(202);
        request.setDiagnosis("Viral infection");
        request.setPrescription("Paracetamol");
        request.setNotes("Stay hydrated");
        request.setAttachmentUrl("https://files.test/report.pdf");
        request.setFollowUpDate(LocalDate.now().plusDays(3));
        return request;
    }

    private MedicalRecord createRecord() {
        return MedicalRecord.builder()
                .recordId(1)
                .appointmentId(500)
                .patientId(101)
                .providerId(202)
                .diagnosis("Viral infection")
                .prescription("Paracetamol")
                .notes("Stay hydrated")
                .attachmentUrl("https://files.test/report.pdf")
                .followUpDate(LocalDate.now().plusDays(3))
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 9, 10))
                .build();
    }
}
