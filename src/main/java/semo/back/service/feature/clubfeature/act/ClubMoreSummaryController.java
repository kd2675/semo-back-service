package semo.back.service.feature.clubfeature.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.clubfeature.biz.ClubMoreSummaryService;
import semo.back.service.feature.clubfeature.vo.ClubMoreSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/summary")
@RequiredArgsConstructor
public class ClubMoreSummaryController {
    private final ClubMoreSummaryService clubMoreSummaryService;

    @GetMapping
    public ResponseDataDTO<ClubMoreSummaryResponse> getSummary(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubMoreSummaryService.getSummary(clubId, requireUserKey(userContext)),
                "더보기 운영 요약 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
