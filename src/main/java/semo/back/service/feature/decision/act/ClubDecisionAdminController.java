package semo.back.service.feature.decision.act;

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
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.decision.biz.ClubDecisionService;
import semo.back.service.feature.decision.vo.ClubDecisionAdminCenterResponse;
import semo.back.service.feature.decision.vo.DecisionRecordResponse;
import semo.back.service.feature.decision.vo.UpsertDecisionRecordRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/more/decisions")
@RequiredArgsConstructor
public class ClubDecisionAdminController {
    private final ClubDecisionService clubDecisionService;

    @GetMapping
    public ResponseDataDTO<ClubDecisionAdminCenterResponse> getAdminCenter(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.getAdminCenter(clubId, requireUserKey(userContext)),
                "회의록·결정 운영 센터 조회 성공"
        );
    }

    @PostMapping("/records")
    public ResponseDataDTO<DecisionRecordResponse> createRecord(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertDecisionRecordRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.createRecord(clubId, requireUserKey(userContext), request),
                "회의록·결정 초안 생성 성공"
        );
    }

    @PutMapping("/records/{decisionRecordId}")
    public ResponseDataDTO<DecisionRecordResponse> updateRecord(
            @PathVariable Long clubId,
            @PathVariable Long decisionRecordId,
            @Valid @RequestBody UpsertDecisionRecordRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.updateRecord(
                        clubId,
                        decisionRecordId,
                        requireUserKey(userContext),
                        request
                ),
                "회의록·결정 초안 수정 성공"
        );
    }

    @PutMapping("/records/{decisionRecordId}/confirm")
    public ResponseDataDTO<DecisionRecordResponse> confirmRecord(
            @PathVariable Long clubId,
            @PathVariable Long decisionRecordId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.confirmRecord(clubId, decisionRecordId, requireUserKey(userContext)),
                "회의록·결정 확정 성공"
        );
    }

    @PutMapping("/records/{decisionRecordId}/archive")
    public ResponseDataDTO<DecisionRecordResponse> archiveRecord(
            @PathVariable Long clubId,
            @PathVariable Long decisionRecordId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubDecisionService.archiveRecord(clubId, decisionRecordId, requireUserKey(userContext)),
                "회의록·결정 보관 성공"
        );
    }

    @DeleteMapping("/records/{decisionRecordId}")
    public ResponseDataDTO<Boolean> deleteDraft(
            @PathVariable Long clubId,
            @PathVariable Long decisionRecordId,
            UserContext userContext
    ) {
        clubDecisionService.deleteDraft(clubId, decisionRecordId, requireUserKey(userContext));
        return ResponseDataDTO.of(true, "회의록·결정 초안 삭제 성공");
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
