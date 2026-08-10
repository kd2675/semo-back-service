package semo.back.service.feature.clubfeature.act;

import auth.common.core.context.RequirePrincipalRole;
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
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.clubfeature.biz.ClubMorePreferenceService;
import semo.back.service.feature.clubfeature.biz.ClubMoreSummaryService;
import semo.back.service.feature.clubfeature.vo.ClubMorePreferenceResponse;
import semo.back.service.feature.clubfeature.vo.ClubMoreSummaryResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubMorePreferenceRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more")
@RequiredArgsConstructor
public class ClubMoreSummaryController {
    private final ClubMoreSummaryService clubMoreSummaryService;
    private final ClubMorePreferenceService clubMorePreferenceService;

    @GetMapping("/summary")
    public ResponseDataDTO<ClubMoreSummaryResponse> getSummary(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubMoreSummaryService.getSummary(clubId, requireUserKey(userContext)),
                "더보기 운영 요약 조회 성공"
        );
    }

    @PutMapping("/preferences/{featureKey}")
    public ResponseDataDTO<ClubMorePreferenceResponse> updateFavorite(
            @PathVariable Long clubId,
            @PathVariable String featureKey,
            @Valid @RequestBody UpdateClubMorePreferenceRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubMorePreferenceService.updateFavorite(
                        clubId,
                        featureKey,
                        requireUserKey(userContext),
                        request
                ),
                "더보기 즐겨찾기 저장 성공"
        );
    }

    @PostMapping("/preferences/{featureKey}/usage")
    public ResponseDataDTO<ClubMorePreferenceResponse> markUsed(
            @PathVariable Long clubId,
            @PathVariable String featureKey,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubMorePreferenceService.markUsed(
                        clubId,
                        featureKey,
                        requireUserKey(userContext)
                ),
                "더보기 최근 사용 기록 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
