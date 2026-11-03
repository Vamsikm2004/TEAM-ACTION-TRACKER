
package com.teamactiontracker.backend.service;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import com.teamactiontracker.backend.dto.ActionCommentResponse;
import com.teamactiontracker.backend.dto.CreateActionCommentRequest;
import com.teamactiontracker.backend.model.ActionComment;
import com.teamactiontracker.backend.model.ActionItem;
import com.teamactiontracker.backend.model.User;
import com.teamactiontracker.backend.repository.ActionCommentRepository;
import com.teamactiontracker.backend.repository.ActionItemRepository;
import com.teamactiontracker.backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActionCommentServiceTest {

    private ActionCommentRepository commentRepository;
    private ActionItemRepository actionItemRepository;
    private UserRepository userRepository;
    private ActionCommentService service;

    private Authentication authentication;

    private User manager;
    private User assignedMember;
    private User otherMember;
    private ActionItem actionItem;

    @BeforeEach
    void setUp() {
        commentRepository = mock(ActionCommentRepository.class);
        actionItemRepository = mock(ActionItemRepository.class);
        userRepository = mock(UserRepository.class);

        service = new ActionCommentService(
                commentRepository,
                actionItemRepository,
                userRepository
        );

        authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal())
                .thenReturn("manager@example.com");

        manager = mock(User.class);
        when(manager.getUserId()).thenReturn(1L);
        when(manager.getEmail()).thenReturn("manager@example.com");
        when(manager.getName()).thenReturn("Manager");
        when(manager.getRole()).thenReturn("MANAGER");
        when(manager.getActiveStatus()).thenReturn(true);

        assignedMember = mock(User.class);
        when(assignedMember.getUserId()).thenReturn(2L);
        when(assignedMember.getEmail()).thenReturn("member@example.com");
        when(assignedMember.getName()).thenReturn("Team Member");
        when(assignedMember.getRole()).thenReturn("TEAM_MEMBER");
        when(assignedMember.getActiveStatus()).thenReturn(true);

        otherMember = mock(User.class);
        when(otherMember.getUserId()).thenReturn(3L);
        when(otherMember.getEmail()).thenReturn("other@example.com");
        when(otherMember.getName()).thenReturn("Other Member");
        when(otherMember.getRole()).thenReturn("TEAM_MEMBER");
        when(otherMember.getActiveStatus()).thenReturn(true);

        actionItem = mock(ActionItem.class);
        when(actionItem.getActionItemId()).thenReturn(10L);
        when(actionItem.getTitle()).thenReturn("Test action");
        when(actionItem.getAssignee()).thenReturn(assignedMember);
    }

    // -------------------- VIEW COMMENTS --------------------

    @Test
    void managerCanViewComments() {
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));
        when(commentRepository
                .findByActionItem_ActionItemIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());

        List<ActionCommentResponse> result =
                service.getComments(10L, authentication);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(commentRepository)
                .findByActionItem_ActionItemIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void assignedTeamMemberCanViewComments() {
        when(authentication.getPrincipal()).thenReturn("member@example.com");
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(assignedMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));
        when(commentRepository
                .findByActionItem_ActionItemIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of());

        List<ActionCommentResponse> result =
                service.getComments(10L, authentication);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void unrelatedTeamMemberCannotViewComments() {
        when(authentication.getPrincipal()).thenReturn("other@example.com");
        when(userRepository.findByEmailIgnoreCase("other@example.com"))
                .thenReturn(Optional.of(otherMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));

        assertThrows(
                AccessDeniedException.class,
                () -> service.getComments(10L, authentication)
        );

        verify(commentRepository, never())
                .findByActionItem_ActionItemIdOrderByCreatedAtAsc(any());
    }

    @Test
    void missingActionItemIsRejectedWhenViewingComments() {
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getComments(999L, authentication)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.getComments(10L, authentication)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void inactiveUserIsRejected() {
        User inactiveUser = mock(User.class);
        when(inactiveUser.getActiveStatus()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(inactiveUser));

        assertThrows(
                AccessDeniedException.class,
                () -> service.getComments(10L, authentication)
        );

        verifyNoInteractions(actionItemRepository);
    }

    // -------------------- CREATE COMMENTS --------------------

    @Test
    void managerCanCreateComment() {
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));

        CreateActionCommentRequest request =
                createRequest(10L, "Manager comment");

        when(commentRepository.save(any(ActionComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActionCommentResponse response =
                service.addComment(request, authentication);

        assertNotNull(response);
        assertEquals("Manager comment", response.comment());
        assertEquals("Manager", response.user().name());

        verify(commentRepository).save(argThat(comment ->
                "Manager comment".equals(comment.getComment())
                && comment.getUser() == manager
                && comment.getActionItem() == actionItem
        ));
    }

    @Test
    void assignedTeamMemberCanCreateComment() {
        when(authentication.getPrincipal()).thenReturn("member@example.com");
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(assignedMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));

        CreateActionCommentRequest request =
                createRequest(10L, "Member comment");

        when(commentRepository.save(any(ActionComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActionCommentResponse response =
                service.addComment(request, authentication);

        assertNotNull(response);
        assertEquals("Member comment", response.comment());
        assertEquals("Team Member", response.user().name());

        verify(commentRepository).save(argThat(comment ->
                "Member comment".equals(comment.getComment())
                && comment.getUser() == assignedMember
                && comment.getActionItem() == actionItem
        ));
    }

    @Test
    void commentIsTrimmedBeforeSaving() {
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));

        CreateActionCommentRequest request =
                createRequest(10L, "  Follow-up required  ");

        when(commentRepository.save(any(ActionComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActionCommentResponse response =
                service.addComment(request, authentication);

        assertEquals("Follow-up required", response.comment());

        verify(commentRepository).save(argThat(comment ->
                "Follow-up required".equals(comment.getComment())
                && comment.getUser() == manager
                && comment.getActionItem() == actionItem
        ));
    }

    @Test
    void unrelatedTeamMemberCannotCreateComment() {
        when(authentication.getPrincipal()).thenReturn("other@example.com");
        when(userRepository.findByEmailIgnoreCase("other@example.com"))
                .thenReturn(Optional.of(otherMember));
        when(actionItemRepository.findById(10L))
                .thenReturn(Optional.of(actionItem));

        CreateActionCommentRequest request =
                createRequest(10L, "Unauthorized comment");

        assertThrows(
                AccessDeniedException.class,
                () -> service.addComment(request, authentication)
        );

        verify(commentRepository, never()).save(any(ActionComment.class));
    }

    @Test
    void missingActionItemIsRejectedWhenCreatingComment() {
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(manager));
        when(actionItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        CreateActionCommentRequest request =
                createRequest(999L, "Comment for missing item");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.addComment(request, authentication)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(commentRepository, never()).save(any(ActionComment.class));
    }

    @Test
    void unauthenticatedUserCannotCreateComment() {
        when(authentication.isAuthenticated()).thenReturn(false);

        CreateActionCommentRequest request =
                createRequest(10L, "Comment");

        assertThrows(
                AccessDeniedException.class,
                () -> service.addComment(request, authentication)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void inactiveUserCannotCreateComment() {
        User inactiveUser = mock(User.class);
        when(inactiveUser.getActiveStatus()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("manager@example.com"))
                .thenReturn(Optional.of(inactiveUser));

        CreateActionCommentRequest request =
                createRequest(10L, "Comment");

        assertThrows(
                AccessDeniedException.class,
                () -> service.addComment(request, authentication)
        );

        verifyNoInteractions(actionItemRepository);
        verifyNoInteractions(commentRepository);
    }

    // -------------------- TEST HELPER --------------------

    private CreateActionCommentRequest createRequest(
            Long actionItemId,
            String comment) {

        CreateActionCommentRequest request =
                new CreateActionCommentRequest();

        request.setActionItemId(actionItemId);
        request.setComment(comment);

        return request;
    }
}
