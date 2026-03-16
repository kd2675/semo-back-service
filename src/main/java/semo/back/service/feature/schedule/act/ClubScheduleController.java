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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.ClubScheduleResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteUpsertResponse;
import semo.back.service.feature.schedule.vo.SubmitScheduleVoteSelectionRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/schedule")
@RequiredArgsConstructor
public class ClubScheduleController {
    private final ClubScheduleService clubScheduleService;

    @GetMapping
    public ResponseDataDTO<ClubScheduleResponse> getClubSchedule(
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.getClubSchedule(clubId, requireUserKey(userContext), year, month),
                "일정 관리 조회 성공"
        );
    }

    @GetMapping("/events/{eventId}")
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

    @PostMapping("/events")
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

    @PutMapping("/events/{eventId}")
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

    @DeleteMapping("/events/{eventId}")
    public ResponseDataDTO<Void> deleteScheduleEvent(
            @PathVariable Long clubId,
            @PathVariable Long eventId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubScheduleService.deleteScheduleEvent(clubId, eventId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "일정 삭제 성공");
    }

    @PutMapping("/events/{eventId}/participation")
    public ResponseDataDTO<ScheduleEventDetailResponse> updateScheduleEventParticipation(
            @PathVariable Long clubId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateScheduleEventParticipationRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.updateScheduleEventParticipation(clubId, eventId, requireUserKey(userContext), request),
                "일정 참석 상태 저장 성공"
        );
    }

    @GetMapping("/votes/{voteId}")
    public ResponseDataDTO<ScheduleVoteDetailResponse> getScheduleVoteDetail(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.getScheduleVoteDetail(clubId, voteId, requireUserKey(userContext)),
                "투표 상세 조회 성공"
        );
    }

    @PostMapping("/votes")
    public ResponseDataDTO<ScheduleVoteUpsertResponse> createScheduleVote(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertScheduleVoteRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.createScheduleVote(clubId, requireUserKey(userContext), request),
                "투표 생성 성공"
        );
    }

    @PutMapping("/votes/{voteId}")
    public ResponseDataDTO<ScheduleVoteUpsertResponse> updateScheduleVote(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            @Valid @RequestBody UpsertScheduleVoteRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.updateScheduleVote(clubId, voteId, requireUserKey(userContext), request),
                "투표 수정 성공"
        );
    }

    @DeleteMapping("/votes/{voteId}")
    public ResponseDataDTO<Void> deleteScheduleVote(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubScheduleService.deleteScheduleVote(clubId, voteId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "투표 삭제 성공");
    }

    @PutMapping("/votes/{voteId}/selection")
    public ResponseDataDTO<ScheduleVoteDetailResponse> submitScheduleVoteSelection(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            @Valid @RequestBody SubmitScheduleVoteSelectionRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.submitScheduleVoteSelection(clubId, voteId, requireUserKey(userContext), request),
                "투표 저장 성공"
        );
    }

    @PutMapping("/votes/{voteId}/close")
    public ResponseDataDTO<ScheduleVoteDetailResponse> closeScheduleVote(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubScheduleService.closeScheduleVote(clubId, voteId, requireUserKey(userContext)),
                "투표 종료 성공"
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
