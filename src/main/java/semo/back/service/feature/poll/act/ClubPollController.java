package semo.back.service.feature.poll.act;

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
import semo.back.service.feature.poll.biz.ClubPollService;
import semo.back.service.feature.poll.vo.ClubPollHomeResponse;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.ScheduleVoteDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteUpsertResponse;
import semo.back.service.feature.schedule.vo.SubmitScheduleVoteSelectionRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/polls")
@RequiredArgsConstructor
public class ClubPollController {
    private final ClubPollService clubPollService;
    private final ClubScheduleService clubScheduleService;

    @GetMapping
    public ResponseDataDTO<ClubPollHomeResponse> getPollHome(
            @PathVariable Long clubId,
            @RequestParam(required = false) String query,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubPollService.getPollHome(clubId, requireUserKey(userContext), query),
                "투표 홈 조회 성공"
        );
    }

    @GetMapping("/{voteId}")
    public ResponseDataDTO<ScheduleVoteDetailResponse> getPollDetail(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
        return ResponseDataDTO.of(
                clubScheduleService.getScheduleVoteDetail(clubId, voteId, requireUserKey(userContext)),
                "투표 상세 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ScheduleVoteUpsertResponse> createPoll(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertScheduleVoteRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
        return ResponseDataDTO.of(
                clubScheduleService.createScheduleVote(clubId, requireUserKey(userContext), request),
                "투표 생성 성공"
        );
    }

    @PutMapping("/{voteId}")
    public ResponseDataDTO<ScheduleVoteUpsertResponse> updatePoll(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            @Valid @RequestBody UpsertScheduleVoteRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
        return ResponseDataDTO.of(
                clubScheduleService.updateScheduleVote(clubId, voteId, requireUserKey(userContext), request),
                "투표 수정 성공"
        );
    }

    @DeleteMapping("/{voteId}")
    public ResponseDataDTO<Void> deletePoll(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
        clubScheduleService.deleteScheduleVote(clubId, voteId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "투표 삭제 성공");
    }

    @PutMapping("/{voteId}/selection")
    public ResponseDataDTO<ScheduleVoteDetailResponse> submitPollSelection(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            @Valid @RequestBody SubmitScheduleVoteSelectionRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
        return ResponseDataDTO.of(
                clubScheduleService.submitScheduleVoteSelection(clubId, voteId, requireUserKey(userContext), request),
                "투표 저장 성공"
        );
    }

    @PutMapping("/{voteId}/close")
    public ResponseDataDTO<ScheduleVoteDetailResponse> closePoll(
            @PathVariable Long clubId,
            @PathVariable Long voteId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubPollService.requirePollFeature(clubId);
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
