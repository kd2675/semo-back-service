package semo.back.service.feature.tournament.act;

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
import semo.back.service.feature.tournament.biz.ClubTournamentService;
import semo.back.service.feature.tournament.vo.ClubAdminTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ReviewTournamentRecordRequest;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubTournamentAdminController {
    private final ClubTournamentService clubTournamentService;

    @GetMapping("/admin/more/tournaments")
    public ResponseDataDTO<ClubAdminTournamentHomeResponse> getAdminTournamentHome(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTournamentService.getAdminTournamentHome(clubId, requireUserKey(userContext)),
                "대회 관리 홈 조회 성공"
        );
    }

    @PutMapping("/admin/more/tournaments/{tournamentRecordId}/review")
    public ResponseDataDTO<TournamentDetailResponse> reviewTournament(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @Valid @RequestBody ReviewTournamentRecordRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTournamentService.reviewTournament(
                        clubId,
                        tournamentRecordId,
                        requireUserKey(userContext),
                        request
                ),
                "대회 승인 검토 성공"
        );
    }

    @DeleteMapping("/admin/more/tournaments/{tournamentRecordId}")
    public ResponseDataDTO<Void> deleteTournament(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        clubTournamentService.deleteTournament(clubId, tournamentRecordId, requireUserKey(userContext));
        return ResponseDataDTO.of(null, "대회 삭제 성공");
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
