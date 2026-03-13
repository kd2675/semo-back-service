package semo.back.service.feature.clubfeature.act;

import auth.common.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/features")
@RequiredArgsConstructor
public class ClubFeatureController {
    private final ClubFeatureService clubFeatureService;

    @GetMapping
    public ResponseDataDTO<List<ClubFeatureResponse>> getClubFeatures(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubFeatureService.getClubFeatures(clubId, userKey),
                "클럽 기능 조회 성공"
        );
    }

    @PutMapping
    public ResponseDataDTO<List<ClubFeatureResponse>> updateClubFeatures(
            @PathVariable Long clubId,
            @RequestBody UpdateClubFeaturesRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubFeatureService.updateClubFeatures(clubId, userKey, request),
                "클럽 기능 설정 저장 성공"
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
