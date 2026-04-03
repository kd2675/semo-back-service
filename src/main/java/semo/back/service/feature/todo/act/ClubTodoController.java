package semo.back.service.feature.todo.act;

import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.todo.biz.ClubTodoService;
import semo.back.service.feature.todo.vo.ClubAdminTodoResponse;
import semo.back.service.feature.todo.vo.ClubTodoResponse;
import semo.back.service.feature.todo.vo.CreateClubTodoRequest;
import semo.back.service.feature.todo.vo.TodoActionResponse;
import semo.back.service.feature.todo.vo.TodoSummaryResponse;
import semo.back.service.feature.todo.vo.UpdateClubTodoRequest;
import semo.back.service.feature.todo.vo.UpdateTodoStatusRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubTodoController {
    private final ClubTodoService clubTodoService;

    @GetMapping("/more/todos")
    public ResponseDataDTO<ClubTodoResponse> getTodos(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.getTodos(clubId, requireUserKey(userContext)),
                "할 일 조회 성공"
        );
    }

    @PostMapping("/more/todos/{todoItemId}/claim")
    public ResponseDataDTO<TodoActionResponse> claimTodo(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.claimTodo(clubId, todoItemId, requireUserKey(userContext)),
                "업무 맡기 성공"
        );
    }

    @PostMapping("/more/todos/{todoItemId}/complete")
    public ResponseDataDTO<TodoActionResponse> completeTodo(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.completeTodo(clubId, todoItemId, requireUserKey(userContext)),
                "업무 완료 처리 성공"
        );
    }

    @GetMapping("/admin/more/todos")
    public ResponseDataDTO<ClubAdminTodoResponse> getAdminTodos(
            @PathVariable Long clubId,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String assignmentFilter,
            @RequestParam(required = false) Long cursorTodoItemId,
            @RequestParam(required = false) Integer size,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.getAdminTodos(
                        clubId,
                        requireUserKey(userContext),
                        statusFilter,
                        assignmentFilter,
                        cursorTodoItemId,
                        size
                ),
                "할 일 운영 조회 성공"
        );
    }

    @PostMapping("/admin/more/todos")
    public ResponseDataDTO<TodoSummaryResponse> createTodo(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubTodoRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.createTodo(clubId, requireUserKey(userContext), request),
                "할 일 등록 성공"
        );
    }

    @PutMapping("/admin/more/todos/{todoItemId}")
    public ResponseDataDTO<TodoSummaryResponse> updateTodo(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @Valid @RequestBody UpdateClubTodoRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.updateTodo(clubId, todoItemId, requireUserKey(userContext), request),
                "할 일 수정 성공"
        );
    }

    @PutMapping("/admin/more/todos/{todoItemId}/status")
    public ResponseDataDTO<TodoActionResponse> updateTodoStatus(
            @PathVariable Long clubId,
            @PathVariable Long todoItemId,
            @Valid @RequestBody UpdateTodoStatusRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTodoService.updateTodoStatus(clubId, todoItemId, requireUserKey(userContext), request),
                "할 일 상태 변경 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

    private void requireUserRole(UserContext userContext) {
        if (userContext == null || !userContext.isAuthenticated()) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        if (!userContext.isUser()) {
            throw new SemoException.ForbiddenException("USER role required");
        }
    }
}
