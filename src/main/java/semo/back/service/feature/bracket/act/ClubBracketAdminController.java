package semo.back.service.feature.bracket.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.bracket.biz.ClubBracketService;
import semo.back.service.feature.bracket.vo.BracketDetailResponse;
import semo.back.service.feature.bracket.vo.ClubAdminBracketHomeResponse;
import semo.back.service.feature.bracket.vo.ReviewBracketRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubBracketAdminController {
    private final ClubBracketService clubBracketService;

    @GetMapping("/admin/more/brackets")
    public ResponseDataDTO<ClubAdminBracketHomeResponse> getAdminBracketHome(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubBracketService.getAdminBracketHome(clubId, requireUserKey(userContext)),
                "대진표 관리자 홈 조회 성공"
        );
    }

    @PutMapping("/admin/more/brackets/{bracketRecordId}/review")
    public ResponseDataDTO<BracketDetailResponse> reviewBracket(
            @PathVariable Long clubId,
            @PathVariable Long bracketRecordId,
            @Valid @RequestBody ReviewBracketRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubBracketService.reviewBracket(clubId, bracketRecordId, requireUserKey(userContext), request),
                "대진표 승인 검토 성공"
        );
    }

    @DeleteMapping("/admin/more/brackets/{bracketRecordId}")
    public ResponseDataDTO<Void> deleteBracket(
            @PathVariable Long clubId,
            @PathVariable Long bracketRecordId,
            UserContext userContext
    ) {
        clubBracketService.deleteBracket(clubId, bracketRecordId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "대진표 삭제 성공");
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
