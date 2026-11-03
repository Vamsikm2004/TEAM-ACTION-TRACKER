package com.teamactiontracker.backend.dto;

import com.teamactiontracker.backend.model.ActionHistory;
import com.teamactiontracker.backend.model.User;

import java.time.LocalDateTime;

public record ActionHistoryResponse(
        Long historyId,
        ActionItemSummary actionItem,
        String previousStatus,
        String newStatus,
        String comment,
        UserSummary updatedBy,
        LocalDateTime updatedAt
) {

    public static ActionHistoryResponse from(ActionHistory history) {
        return new ActionHistoryResponse(
                history.getHistoryId(),
                history.getActionItem() == null
                        ? null
                        : new ActionItemSummary(
                                history.getActionItem().getActionItemId(),
                                history.getActionItem().getTitle()
                        ),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getComment(),
                UserSummary.from(history.getUpdatedBy()),
                history.getUpdatedAt()
        );
    }

    public record ActionItemSummary(
            Long actionItemId,
            String title
    ) {}

    public record UserSummary(
            Long userId,
            String name,
            String email,
            String role
    ) {
        public static UserSummary from(User user) {
            if (user == null) {
                return null;
            }

            return new UserSummary(
                    user.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole()
            );
        }
    }
}