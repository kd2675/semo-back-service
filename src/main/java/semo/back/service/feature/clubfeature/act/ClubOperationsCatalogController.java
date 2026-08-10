package semo.back.service.feature.clubfeature.act;

import auth.common.core.context.RequirePrincipalRole;
import auth.common.core.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.clubfeature.biz.ClubOperationsCatalogService;
import semo.back.service.feature.clubfeature.vo.ApplyClubOperationTemplateRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubOperationTemplateResponse;
import semo.back.service.feature.clubfeature.vo.ApplyClubPresetRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubPresetResponse;
import semo.back.service.feature.clubfeature.vo.ClubOperationsCatalogResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/operations-catalog")
@RequiredArgsConstructor
public class ClubOperationsCatalogController {
    private final ClubOperationsCatalogService clubOperationsCatalogService;

    @GetMapping
    public ResponseDataDTO<ClubOperationsCatalogResponse> getCatalog(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubOperationsCatalogService.getCatalog(clubId, requireUserKey(userContext)),
                "운영 프리셋과 템플릿 조회 성공"
        );
    }

    @PostMapping("/presets/{presetKey}/apply")
    public ResponseDataDTO<ApplyClubPresetResponse> applyPreset(
            @PathVariable Long clubId,
            @PathVariable String presetKey,
            @RequestBody(required = false) ApplyClubPresetRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubOperationsCatalogService.applyPreset(
                        clubId,
                        presetKey,
                        requireUserKey(userContext),
                        request
                ),
                "클럽 프리셋 적용 성공"
        );
    }

    @PostMapping("/templates/{templateKey}/apply")
    public ResponseDataDTO<ApplyClubOperationTemplateResponse> applyTemplate(
            @PathVariable Long clubId,
            @PathVariable String templateKey,
            @Valid @RequestBody(required = false) ApplyClubOperationTemplateRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubOperationsCatalogService.applyTemplate(
                        clubId,
                        templateKey,
                        requireUserKey(userContext),
                        request
                ),
                "운영 템플릿 적용 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
