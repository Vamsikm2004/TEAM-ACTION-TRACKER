package com.teamactiontracker.backend.repository;

import com.teamactiontracker.backend.model.ActionItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionItemRepository
        extends JpaRepository<ActionItem, Long> {

    @Override
    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    List<ActionItem> findAll();

    @Override
    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    Optional<ActionItem> findById(Long id);

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    List<ActionItem> findByAssignee_UserId(Long userId);
}