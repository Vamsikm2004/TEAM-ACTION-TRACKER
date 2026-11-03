package com.teamactiontracker.backend.repository;

import com.teamactiontracker.backend.model.ActionHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionHistoryRepository
        extends JpaRepository<ActionHistory, Long> {

    @EntityGraph(attributePaths = {"actionItem", "updatedBy"})
    List<ActionHistory> findByActionItem_ActionItemIdOrderByUpdatedAtDescHistoryIdDesc(
            Long actionItemId);
}