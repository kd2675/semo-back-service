package semo.back.service.feature.schedule.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.poll.biz.ClubPollService;
import semo.back.service.feature.schedule.vo.ClubScheduleVoteSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/schedule/votes/summary")
@RequiredArgsConstructor
public class ClubScheduleVoteSummaryController {
    private final ClubPollService clubPollService;

    @GetMapping
    public ResponseDataDTO<ClubScheduleVoteSummaryResponse> getVoteSummary(
            @PathVariable Long clubId,
            @RequestParam(required = false) String query,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPollService.getVoteSummary(clubId, requireUserKey(userContext), query),
                "투표 요약 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
