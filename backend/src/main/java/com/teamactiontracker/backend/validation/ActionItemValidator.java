package com.teamactiontracker.backend.validation;

import com.teamactiontracker.backend.dto.CreateActionItemRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Component
public class ActionItemValidator {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private static final Set<String> ALLOWED_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("NOT_STARTED", "IN_PROGRESS", "BLOCKED", "COMPLETED");

    public void validate(CreateActionItemRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Action item request is required");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (request.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Title must not exceed 200 characters");
        }

        if (request.getDescription() != null &&
                request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Description must not exceed 2000 characters");
        }

        if (request.getAssigneeId() == null) {
            throw new IllegalArgumentException("Assignee is required");
        }

        String priority = normalize(request.getPriority());

        if (priority == null || !ALLOWED_PRIORITIES.contains(priority)) {
            throw new IllegalArgumentException(
                    "Priority must be LOW, MEDIUM, HIGH, or CRITICAL");
        }

        String status = normalize(request.getStatus());

        if (status != null && !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid action item status");
        }

        if (request.getDueDate() == null) {
            throw new IllegalArgumentException("Due date is required");
        }

        if (request.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Due date cannot be in the past");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}