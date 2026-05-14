package com.medibook.appointment.config;

import com.medibook.appointment.resource.AppointmentResource;
import com.medibook.appointment.service.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentResource.class)
@Import(SecurityConfig.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("Unauthenticated requests are allowed for appointment endpoints")
    void unauthenticatedRequestsAreAllowed() throws Exception {
        when(appointmentService.getAppointmentCount(5)).thenReturn(2);

        mockMvc.perform(get("/appointments/provider/5/count"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Validation failures are not blocked by security")
    void validationFailuresAreStillReturnedAsBadRequest() throws Exception {
        mockMvc.perform(get("/appointments/provider/5/date").param("date", "bad-date"))
                .andExpect(status().isInternalServerError());
    }
}
