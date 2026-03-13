package semo.back.service.feature.club.act;

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
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.ClubCreateResponse;
import semo.back.service.feature.club.vo.ClubBoardResponse;
import semo.back.service.feature.club.vo.ClubProfileResponse;
import semo.back.service.feature.club.vo.ClubScheduleResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.MyClubSummaryResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/semo/v1/clubs")
@RequiredArgsConstructor
public class ClubController {
    private final ClubService clubService;

    @PostMapping
    public ResponseDataDTO<ClubCreateResponse> createClub(
            @Valid @RequestBody CreateClubRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubService.createClub(userKey, userContext.getUserName(), request),
                "클럽 생성 성공"
        );
    }

    @GetMapping("/my")
    public ResponseDataDTO<List<MyClubSummaryResponse>> getMyClubs(UserContext userContext) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubService.getMyClubs(userKey),
                "내 클럽 조회 성공"
        );
    }

    @GetMapping("/{clubId}")
    public ResponseDataDTO<MyClubSummaryResponse> getMyClub(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(
                clubService.getMyClub(clubId, userKey),
                "내 클럽 상세 조회 성공"
        );
    }

    @GetMapping("/{clubId}/board")
    public ResponseDataDTO<ClubBoardResponse> getClubBoard(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(clubService.getClubBoard(clubId, userKey), "클럽 게시판 조회 성공");
    }

    @GetMapping("/{clubId}/schedule")
    public ResponseDataDTO<ClubScheduleResponse> getClubSchedule(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(clubService.getClubSchedule(clubId, userKey), "클럽 일정 조회 성공");
    }

    @GetMapping("/{clubId}/profile")
    public ResponseDataDTO<ClubProfileResponse> getClubProfile(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        String userKey = requireUserKey(userContext);
        return ResponseDataDTO.of(clubService.getClubProfile(clubId, userKey), "클럽 프로필 조회 성공");
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
