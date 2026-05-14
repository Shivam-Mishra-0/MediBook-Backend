package com.medibook.schedule.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.medibook.schedule.support.TestDataFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class SlotRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void validRequestPassesValidationAndDataMethods() {
        SlotRequest first = TestDataFactory.validRequest();
        SlotRequest second = TestDataFactory.validRequest();
        second.setRecurrenceEndDate(first.getDate().plusDays(7));

        assertThat(validator.validate(first)).isEmpty();
        assertThat(first).isEqualTo(TestDataFactory.validRequest());
        assertThat(first.hashCode()).isEqualTo(TestDataFactory.validRequest().hashCode());
        assertThat(first.toString()).contains("providerId=10");
        assertThat(second.getRecurrenceEndDate())
                .isEqualTo(first.getDate().plusDays(7));
    }

    @Test
    void missingRequiredFieldsReturnExpectedMessages() {
        SlotRequest request = new SlotRequest();

        Map<String, String> errors = messageMap(validator.validate(request));

        assertThat(errors)
                .containsEntry("providerId", "Provider ID must be greater than 0")
                .containsEntry("date", "Date is required")
                .containsEntry("startTime", "Start time is required")
                .containsEntry("endTime", "End time is required")
                .containsEntry("durationMinutes", "Duration must be at least 1 minute");
    }

    @Test
    void customAndDateValidationsReturnExpectedMessages() {
        SlotRequest request = TestDataFactory.validRequest();
        request.setDate(LocalDate.now().minusDays(1));
        request.setEndTime(request.getStartTime());
        request.setRecurrence("monthly");

        Map<String, String> errors = messageMap(validator.validate(request));

        assertThat(errors)
                .containsEntry("date", "Date cannot be in the past")
                .containsEntry("timeRangeValid", "End time must be after start time")
                .containsEntry("recurrenceValid", "Recurrence must be NONE, DAILY, or WEEKLY");
    }

    private Map<String, String> messageMap(
            Set<ConstraintViolation<SlotRequest>> violations) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<SlotRequest> violation : violations) {
            errors.put(
                    violation.getPropertyPath().toString(),
                    violation.getMessage()
            );
        }
        return errors;
    }
}
