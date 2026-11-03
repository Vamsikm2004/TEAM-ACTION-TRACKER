
package com.teamactiontracker.backend.service;

import com.teamactiontracker.backend.dto.CreateActionItemRequest;
import com.teamactiontracker.backend.model.ActionItem;
import com.teamactiontracker.backend.model.User;
import com.teamactiontracker.backend.repository.ActionHistoryRepository;
import com.teamactiontracker.backend.repository.ActionItemRepository;
import com.teamactiontracker.backend.repository.UserRepository;
import com.teamactiontracker.backend.validation.ActionItemValidator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActionItemServiceTest {

    private ActionItemRepository actionItemRepository;
    private UserRepository userRepository;
    private ActionHistoryRepository actionHistoryRepository;
    private ActionItemService actionItemService;

    private User manager;
    private User teamMember;
    private User anotherMember;

    @BeforeEach
    void setUp() {
        actionItemRepository = mock(ActionItemRepository.class);
        userRepository = mock(UserRepository.class);
        actionHistoryRepository = mock(ActionHistoryRepository.class);

        actionItemService = new ActionItemService(
                actionItemRepository,
                userRepository,
                actionHistoryRepository,
                new ActionItemValidator()
        );

        manager = createUser(
                1L, "Manager", "manager@example.com", "MANAGER", true);

        teamMember = createUser(
                2L, "Team Member", "member@example.com",
                "TEAM_MEMBER", true);

        anotherMember = createUser(
                3L, "Another Member", "another@example.com",
                "TEAM_MEMBER", true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCanRetrieveAllActionItems() {
        setAuthenticatedUser(
                "manager@example.com", "ROLE_MANAGER");

        List<ActionItem> expectedItems = List.of(
                createActionItem(10L, teamMember),
                createActionItem(11L, anotherMember)
        );

        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findAll()).thenReturn(expectedItems);

        List<ActionItem> result = actionItemService.getAllActionItems();

        assertEquals(2, result.size());
        assertSame(expectedItems, result);
        verify(actionItemRepository).findAll();
        verify(actionItemRepository, never())
                .findByAssignee_UserId(any());
    }

    @Test
    void teamMemberCanRetrieveOnlyTheirAssignedActionItems() {
        setAuthenticatedUser(
                "member@example.com", "ROLE_TEAM_MEMBER");

        List<ActionItem> expectedItems = List.of(
                createActionItem(10L, teamMember)
        );

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(teamMember));
        when(actionItemRepository.findByAssignee_UserId(2L))
                .thenReturn(expectedItems);

        List<ActionItem> result = actionItemService.getAllActionItems();

        assertEquals(1, result.size());
        assertSame(expectedItems, result);
        verify(actionItemRepository).findByAssignee_UserId(2L);
        verify(actionItemRepository, never()).findAll();
    }

    @Test
    void teamMemberCannotAccessAnotherMembersActionItem() {
        setAuthenticatedUser(
                "member@example.com", "ROLE_TEAM_MEMBER");

        ActionItem anotherMembersItem =
                createActionItem(10L, anotherMember);

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(teamMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(anotherMembersItem));

        assertThrows(
                AccessDeniedException.class,
                () -> actionItemService.getActionItemById(10L)
        );
    }

    @Test
    void teamMemberCannotAccessAnotherMembersHistory() {
        setAuthenticatedUser(
                "member@example.com", "ROLE_TEAM_MEMBER");

        ActionItem anotherMembersItem =
                createActionItem(10L, anotherMember);

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(teamMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(anotherMembersItem));

        assertThrows(
                AccessDeniedException.class,
                () -> actionItemService.getActionItemHistory(10L)
        );

        verify(actionHistoryRepository, never())
                .findByActionItem_ActionItemIdOrderByUpdatedAtDescHistoryIdDesc(10L);
    }

    @Test
    void inactiveUserIsDeniedAccess() {
        User inactiveUser = createUser(
                4L, "Inactive User", "inactive@example.com",
                "TEAM_MEMBER", false);

        setAuthenticatedUser(
                "inactive@example.com", "ROLE_TEAM_MEMBER");

        when(userRepository.findByEmailIgnoreCase("inactive@example.com"))
                .thenReturn(Optional.of(inactiveUser));

        assertThrows(
                AccessDeniedException.class,
                () -> actionItemService.getAllActionItems()
        );

        verify(actionItemRepository, never()).findAll();
        verify(actionItemRepository, never())
                .findByAssignee_UserId(any());
    }

    @Test
    void actionItemCreatorComesFromAuthenticatedManagerNotRequest() {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "manager@example.com",
                        "test-password",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                );

        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(teamMember));
        when(actionItemRepository.save(any(ActionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateActionItemRequest request = new CreateActionItemRequest();
        request.setTitle("Prepare weekly report");
        request.setDescription("Prepare and submit the weekly report");
        request.setAssigneeId(2L);
        request.setPriority("HIGH");
        request.setStatus("NOT_STARTED");
        request.setDueDate(LocalDate.now().plusDays(3));
        ActionItem savedItem =
                actionItemService.createActionItem(request, authentication);

        assertSame(manager, savedItem.getCreatedBy());
        assertEquals("Prepare weekly report", savedItem.getTitle());
        verify(actionItemRepository).save(any(ActionItem.class));
    }

    @Test
    void teamMemberCannotCreateActionItems() {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "member@example.com",
                        "test-password",
                        List.of(new SimpleGrantedAuthority("ROLE_TEAM_MEMBER"))
                );

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(teamMember));

        CreateActionItemRequest request = new CreateActionItemRequest();
        request.setTitle("Prepare weekly report");
        request.setAssigneeId(2L);
        request.setPriority("HIGH");
        request.setStatus("NOT_STARTED");
        request.setDueDate(LocalDate.now().plusDays(3));

        assertThrows(
                AccessDeniedException.class,
                () -> actionItemService.createActionItem(
                        request, authentication)
        );

        verify(actionItemRepository, never()).save(any(ActionItem.class));
    }

    private User createUser(
            Long id,
            String name,
            String email,
            String role,
            boolean active) {

        User user = new User();
        user.setUserId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setActiveStatus(active);
        return user;
    }

    private ActionItem createActionItem(Long id, User assignee) {
        ActionItem item = new ActionItem();
        item.setActionItemId(id);
        item.setAssignee(assignee);
        item.setStatus("NOT_STARTED");
        item.setTitle("Test action item");
        item.setPriority("HIGH");
        item.setDueDate(LocalDate.now().plusDays(3));
        item.setCreatedBy(manager);
        return item;
    }

    private void setAuthenticatedUser(String email, String role) {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        email,
                        "test-password",
                        List.of(new SimpleGrantedAuthority(role))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}
