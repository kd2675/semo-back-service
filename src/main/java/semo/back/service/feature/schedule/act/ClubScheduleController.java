package semo.back.service.feature.schedule.act;

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
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/schedule/events")
@RequiredArgsConstructor
public class ClubScheduleController {
    private final ClubScheduleService clubScheduleService;

    @GetMapping("/{eventId}")
    public ResponseDataDTO<ScheduleEventDetailResponse> getScheduleEventDetail(
            @PathVariable Long clubId,
            @PathVariable Long eventId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.getScheduleEventDetail(clubId, eventId, requireUserKey(userContext)),
                "일정 상세 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ScheduleEventUpsertResponse> createScheduleEvent(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertScheduleEventRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.createScheduleEvent(clubId, requireUserKey(userContext), request),
                "일정 생성 성공"
        );
    }

    @PutMapping("/{eventId}")
    public ResponseDataDTO<ScheduleEventUpsertResponse> updateScheduleEvent(
            @PathVariable Long clubId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpsertScheduleEventRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.updateScheduleEvent(clubId, eventId, requireUserKey(userContext), request),
                "일정 수정 성공"
        );
    }

    @DeleteMapping("/{eventId}")
    public ResponseDataDTO<Void> deleteScheduleEvent(
            @PathVariable Long clubId,
            @PathVariable Long eventId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubScheduleService.deleteScheduleEvent(clubId, eventId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "일정 삭제 성공");
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
