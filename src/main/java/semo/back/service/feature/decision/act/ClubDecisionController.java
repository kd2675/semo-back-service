package semo.back.service.feature.decision.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.decision.biz.ClubDecisionService;
import semo.back.service.feature.decision.vo.ClubDecisionLogResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/decisions")
@RequiredArgsConstructor
public class ClubDecisionController {
    private final ClubDecisionService clubDecisionService;

    @GetMapping
    public ResponseDataDTO<ClubDecisionLogResponse> getMemberLog(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.getMemberLog(clubId, requireUserKey(userContext)),
                "회의록·결정 조회 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
