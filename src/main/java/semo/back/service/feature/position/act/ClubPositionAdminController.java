package semo.back.service.feature.position.act;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.position.biz.ClubPositionService;
import semo.back.service.feature.position.vo.ClubAdminRoleManagementResponse;
import semo.back.service.feature.position.vo.ClubPositionHistoryResponse;
import semo.back.service.feature.position.vo.ClubPositionDetailResponse;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/more/roles")
@RequiredArgsConstructor
public class ClubPositionAdminController {
    private final ClubPositionService clubPositionService;

    @GetMapping
    public ResponseDataDTO<ClubAdminRoleManagementResponse> getRoleManagement(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPositionService.getRoleManagement(clubId, requireUserKey(userContext)),
                "직책관리 조회 성공"
        );
    }

    @GetMapping("/history")
    public ResponseDataDTO<ClubPositionHistoryResponse> getPositionHistory(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPositionService.getPositionHistory(clubId, requireUserKey(userContext)),
                "직책 보유 이력 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<ClubPositionDetailResponse> createPosition(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubPositionRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPositionService.createPosition(clubId, requireUserKey(userContext), request),
                "직책 생성 성공"
        );
    }

    @GetMapping("/{clubPositionId}")
    public ResponseDataDTO<ClubPositionDetailResponse> getPositionDetail(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPositionService.getPositionDetail(clubId, clubPositionId, requireUserKey(userContext)),
                "직책 상세 조회 성공"
        );
    }

    @PutMapping("/{clubPositionId}")
    public ResponseDataDTO<ClubPositionDetailResponse> updatePosition(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            @Valid @RequestBody UpdateClubPositionRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubPositionService.updatePosition(clubId, clubPositionId, requireUserKey(userContext), request),
                "직책 수정 성공"
        );
    }

    @DeleteMapping("/{clubPositionId}")
    public ResponseDataDTO<Boolean> deletePosition(
            @PathVariable Long clubId,
            @PathVariable Long clubPositionId,
            @RequestParam Long version,
            UserContext userContext
    ) {
        clubPositionService.deletePosition(clubId, clubPositionId, version, requireUserKey(userContext));
        return ResponseDataDTO.of(true, "직책 사용 종료 성공");
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
