package semo.back.service.feature.bracket.act;

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
import semo.back.service.feature.bracket.biz.ClubBracketService;
import semo.back.service.feature.bracket.vo.BracketDetailResponse;
import semo.back.service.feature.bracket.vo.BracketUpsertResponse;
import semo.back.service.feature.bracket.vo.ClubBracketHomeResponse;
import semo.back.service.feature.bracket.vo.UpsertBracketRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/brackets")
@RequiredArgsConstructor
public class ClubBracketController {
    private final ClubBracketService clubBracketService;

    @GetMapping
    public ResponseDataDTO<ClubBracketHomeResponse> getBracketHome(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubBracketService.getBracketHome(clubId, requireUserKey(userContext)),
                "대진표 홈 조회 성공"
        );
    }

    @GetMapping("/{bracketRecordId}")
    public ResponseDataDTO<BracketDetailResponse> getBracketDetail(
            @PathVariable Long clubId,
            @PathVariable Long bracketRecordId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubBracketService.getBracketDetail(clubId, bracketRecordId, requireUserKey(userContext)),
                "대진표 상세 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<BracketUpsertResponse> createBracket(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertBracketRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubBracketService.createBracket(clubId, requireUserKey(userContext), request),
                "대진표 초안 생성 성공"
        );
    }

    @PutMapping("/{bracketRecordId}")
    public ResponseDataDTO<BracketUpsertResponse> updateBracket(
            @PathVariable Long clubId,
            @PathVariable Long bracketRecordId,
            @Valid @RequestBody UpsertBracketRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubBracketService.updateBracket(clubId, bracketRecordId, requireUserKey(userContext), request),
                "대진표 초안 수정 성공"
        );
    }

    @PutMapping("/{bracketRecordId}/submit")
    public ResponseDataDTO<BracketDetailResponse> submitBracket(
            @PathVariable Long clubId,
            @PathVariable Long bracketRecordId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubBracketService.submitBracket(clubId, bracketRecordId, requireUserKey(userContext)),
                "대진표 제출 성공"
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
