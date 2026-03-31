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
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.UpdateTournamentEntriesRequest;
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

    @PutMapping("/admin/more/tournaments/{tournamentRecordId}/applications/{tournamentApplicationId}/review")
    public ResponseDataDTO<TournamentDetailResponse> reviewApplication(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @PathVariable Long tournamentApplicationId,
            @Valid @RequestBody ReviewTournamentApplicationRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTournamentService.reviewApplication(
                        clubId,
                        tournamentRecordId,
                        tournamentApplicationId,
                        requireUserKey(userContext),
                        request
                ),
                "참가 신청 검토 성공"
        );
    }

    @PutMapping("/admin/more/tournaments/{tournamentRecordId}/entries")
    public ResponseDataDTO<TournamentDetailResponse> updateEntries(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @Valid @RequestBody UpdateTournamentEntriesRequest request,
            UserContext userContext
    ) {
        requireUserRole(userContext);
        return ResponseDataDTO.of(
                clubTournamentService.updateEntries(clubId, tournamentRecordId, requireUserKey(userContext), request),
                "엔트리 편성 저장 성공"
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
