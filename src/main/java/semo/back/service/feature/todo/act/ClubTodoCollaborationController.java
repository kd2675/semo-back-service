package semo.back.service.feature.todo.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.todo.biz.ClubTodoCollaborationService;
import semo.back.service.feature.todo.vo.CreateTodoChecklistItemRequest;
import semo.back.service.feature.todo.vo.CreateTodoCommentRequest;
import semo.back.service.feature.todo.vo.TodoChecklistItemResponse;
import semo.back.service.feature.todo.vo.TodoCollaborationResponse;
import semo.back.service.feature.todo.vo.TodoCommentResponse;
import semo.back.service.feature.todo.vo.UpdateTodoChecklistItemRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/todos/{todoItemId}")
@RequiredArgsConstructor
public class ClubTodoCollaborationController {
    private final ClubTodoCollaborationService clubTodoCollaborationService;

    @GetMapping("/collaboration")
    public ResponseDataDTO<TodoCollaborationResponse> getCollaboration(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTodoCollaborationService.getCollaboration(
                        clubId,
                        todoItemId,
                        requireUserKey(userContext)
                ),
                "업무 협업 정보 조회 성공"
        );
    }

    @PostMapping("/checklist")
    public ResponseDataDTO<TodoChecklistItemResponse> addChecklistItem(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @Valid @RequestBody CreateTodoChecklistItemRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTodoCollaborationService.addChecklistItem(
                        clubId,
                        todoItemId,
                        requireUserKey(userContext),
                        request
                ),
                "업무 체크리스트 추가 성공"
        );
    }

    @PutMapping("/checklist/{todoChecklistItemId}")
    public ResponseDataDTO<TodoChecklistItemResponse> updateChecklistItem(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @PathVariable Long todoChecklistItemId,
            @Valid @RequestBody UpdateTodoChecklistItemRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTodoCollaborationService.updateChecklistItem(
                        clubId,
                        todoItemId,
                        todoChecklistItemId,
                        requireUserKey(userContext),
                        request
                ),
                "업무 체크리스트 수정 성공"
        );
    }

    @DeleteMapping("/checklist/{todoChecklistItemId}")
    public ResponseDataDTO<Void> deleteChecklistItem(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @PathVariable Long todoChecklistItemId,
            UserContext userContext
    ) {
        clubTodoCollaborationService.deleteChecklistItem(
                clubId,
                todoItemId,
                todoChecklistItemId,
                requireUserKey(userContext)
        );
        return ResponseDataDTO.of(null, "업무 체크리스트 삭제 성공");
    }

    @PostMapping("/comments")
    public ResponseDataDTO<TodoCommentResponse> addComment(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @Valid @RequestBody CreateTodoCommentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTodoCollaborationService.addComment(
                        clubId,
                        todoItemId,
                        requireUserKey(userContext),
                        request
                ),
                "업무 댓글 등록 성공"
        );
    }

    @DeleteMapping("/comments/{todoCommentId}")
    public ResponseDataDTO<Void> deleteComment(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @PathVariable Long todoCommentId,
            UserContext userContext
    ) {
        clubTodoCollaborationService.deleteComment(
                clubId,
                todoItemId,
                todoCommentId,
                requireUserKey(userContext)
        );
        return ResponseDataDTO.of(null, "업무 댓글 삭제 성공");
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
