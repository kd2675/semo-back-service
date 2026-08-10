package semo.back.service.feature.attachment.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.FinanceRequest;
import semo.back.service.database.pub.entity.ResourceAttachment;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.FinanceRequestRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.finance.biz.policy.ClubFinancePermissionService;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;

@Component
@RequiredArgsConstructor
public class ResourceAttachmentPolicy {
    public static final String RESOURCE_TODO_ITEM = "TODO_ITEM";
    public static final String RESOURCE_FINANCE_REQUEST = "FINANCE_REQUEST";
    public static final String RESOURCE_FEEDBACK = "FEEDBACK";

    private final TodoItemRepository todoItemRepository;
    private final FinanceRequestRepository financeRequestRepository;
    private final ClubFeedbackRepository clubFeedbackRepository;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final ClubFinancePermissionService clubFinancePermissionService;

    public String requireCanAttach(
            ClubAccessResolver.ClubAccess access,
            String resourceType,
            Long resourceId
    ) {
        return switch (resourceType) {
            case RESOURCE_TODO_ITEM -> {
                TodoItem todo = requireTodo(access.club().getClubId(), resourceId);
                boolean ownsTodo = access.clubProfile().getClubProfileId().equals(todo.getCreatedByClubProfileId())
                        || access.clubProfile().getClubProfileId().equals(todo.getAssignedClubProfileId());
                if (!ownsTodo
                        && !clubTodoPermissionService.canCreateTodo(access)
                        && !clubTodoPermissionService.canAssignTodo(access)) {
                    throw new SemoException.ForbiddenException("업무 첨부파일을 등록할 권한이 없습니다.");
                }
                yield "CLUB";
            }
            case RESOURCE_FINANCE_REQUEST -> {
                FinanceRequest financeRequest = requireFinanceRequest(access.club().getClubId(), resourceId);
                boolean requester = access.clubProfile().getClubProfileId()
                        .equals(financeRequest.getRequesterClubProfileId());
                if (!requester && !clubFinancePermissionService.canIssueFinance(access)) {
                    throw new SemoException.ForbiddenException("재정 요청 첨부파일을 등록할 권한이 없습니다.");
                }
                yield "OWNER_AND_OPERATORS";
            }
            case RESOURCE_FEEDBACK -> {
                ClubFeedback feedback = requireFeedback(access.club().getClubId(), resourceId);
                boolean submitter = access.clubProfile().getClubProfileId()
                        .equals(feedback.getSubmitterClubProfileId());
                if (!submitter && !access.isAdmin()) {
                    throw new SemoException.ForbiddenException("피드백 첨부파일을 등록할 권한이 없습니다.");
                }
                yield "OWNER_AND_ADMIN";
            }
            default -> throw new SemoException.ValidationException("지원하지 않는 첨부 대상입니다.");
        };
    }

    public void requireCanView(
            ClubAccessResolver.ClubAccess access,
            String resourceType,
            Long resourceId
    ) {
        switch (resourceType) {
            case RESOURCE_TODO_ITEM -> requireTodo(access.club().getClubId(), resourceId);
            case RESOURCE_FINANCE_REQUEST -> {
                FinanceRequest financeRequest = requireFinanceRequest(access.club().getClubId(), resourceId);
                boolean requester = access.clubProfile().getClubProfileId()
                        .equals(financeRequest.getRequesterClubProfileId());
                if (!requester && !clubFinancePermissionService.canViewAdminFinance(access)) {
                    throw new SemoException.ForbiddenException("재정 요청 첨부파일을 조회할 권한이 없습니다.");
                }
            }
            case RESOURCE_FEEDBACK -> {
                ClubFeedback feedback = requireFeedback(access.club().getClubId(), resourceId);
                boolean submitter = access.clubProfile().getClubProfileId()
                        .equals(feedback.getSubmitterClubProfileId());
                if (!submitter && !access.isAdmin()) {
                    throw new SemoException.ForbiddenException("피드백 첨부파일을 조회할 권한이 없습니다.");
                }
            }
            default -> throw new SemoException.ValidationException("지원하지 않는 첨부 대상입니다.");
        }
    }

    public void requireCanDelete(
            ClubAccessResolver.ClubAccess access,
            ResourceAttachment attachment
    ) {
        if (access.clubProfile().getClubProfileId().equals(attachment.getUploaderClubProfileId())) {
            return;
        }
        switch (attachment.getResourceType()) {
            case RESOURCE_TODO_ITEM -> {
                requireTodo(access.club().getClubId(), attachment.getResourceId());
                if (!clubTodoPermissionService.canDeleteTodo(access)
                        && !clubTodoPermissionService.canAssignTodo(access)) {
                    throw new SemoException.ForbiddenException("업무 첨부파일을 삭제할 권한이 없습니다.");
                }
            }
            case RESOURCE_FINANCE_REQUEST -> {
                requireFinanceRequest(access.club().getClubId(), attachment.getResourceId());
                if (!clubFinancePermissionService.canIssueFinance(access)) {
                    throw new SemoException.ForbiddenException("재정 요청 첨부파일을 삭제할 권한이 없습니다.");
                }
            }
            case RESOURCE_FEEDBACK -> {
                requireFeedback(access.club().getClubId(), attachment.getResourceId());
                if (!access.isAdmin()) {
                    throw new SemoException.ForbiddenException("피드백 첨부파일을 삭제할 권한이 없습니다.");
                }
            }
            default -> throw new SemoException.ValidationException("지원하지 않는 첨부 대상입니다.");
        }
    }

    private TodoItem requireTodo(Long clubId, Long resourceId) {
        return todoItemRepository.findByTodoItemIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", resourceId));
    }

    private FinanceRequest requireFinanceRequest(Long clubId, Long resourceId) {
        return financeRequestRepository.findByFinanceRequestIdAndClubId(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "FinanceRequest",
                        "financeRequestId",
                        resourceId
                ));
    }

    private ClubFeedback requireFeedback(Long clubId, Long resourceId) {
        return clubFeedbackRepository.findByFeedbackIdAndClubIdAndDeletedFalse(resourceId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubFeedback", "feedbackId", resourceId));
    }
}
