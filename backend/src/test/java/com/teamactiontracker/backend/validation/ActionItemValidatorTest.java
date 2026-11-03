package com.teamactiontracker.backend.validation;

import com.teamactiontracker.backend.dto.CreateActionItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionItemValidatorTest {

    private ActionItemValidator validator;
    private CreateActionItemRequest request;

    @BeforeEach
    void setUp() {
        validator = new ActionItemValidator();

        request = new CreateActionItemRequest();
        request.setTitle("Prepare weekly report");
        request.setAssigneeId(1L);        request.setPriority("HIGH");
        request.setStatus("NOT_STARTED");
        request.setDueDate(LocalDate.now().plusDays(3));
    }

    @Test
    void shouldAcceptValidRequest() {
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void shouldRejectBlankTitle() {
        request.setTitle(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );

        assertEquals("Title is required", exception.getMessage());
    }

    @Test
    void shouldRejectTitleLongerThan200Characters() {
        request.setTitle("A".repeat(201));

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectMissingAssignee() {
        request.setAssigneeId(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectInvalidPriority() {
        request.setPriority("URGENT");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectInvalidStatus() {
        request.setStatus("CANCELLED");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldRejectMissingDueDate() {
        request.setDueDate(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );
    }

    @Test
    void shouldAllowNullStatusForDefaulting() {
        request.setStatus(null);

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void shouldRejectPastDueDate() {
        request.setDueDate(LocalDate.now().minusDays(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(request)
        );

        assertEquals("Due date cannot be in the past", exception.getMessage());
    }

    @Test
    void shouldAcceptDueDateToday() {
        request.setDueDate(LocalDate.now());

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void shouldAcceptFutureDueDate() {
        request.setDueDate(LocalDate.now().plusDays(1));

        assertDoesNotThrow(() -> validator.validate(request));
    }
}