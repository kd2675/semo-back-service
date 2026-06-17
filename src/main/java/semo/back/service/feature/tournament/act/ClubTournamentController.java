package semo.back.service.feature.tournament.act;

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
import semo.back.service.feature.tournament.biz.ClubTournamentService;
import semo.back.service.feature.tournament.vo.CancelTournamentRequest;
import semo.back.service.feature.tournament.vo.ClubTournamentHomeResponse;
import semo.back.service.feature.tournament.vo.ReviewTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.SubmitTournamentApplicationRequest;
import semo.back.service.feature.tournament.vo.TournamentDetailResponse;
import semo.back.service.feature.tournament.vo.TournamentUpsertResponse;
import semo.back.service.feature.tournament.vo.UpsertTournamentRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/more/tournaments")
@RequiredArgsConstructor
public class ClubTournamentController {
    private final ClubTournamentService clubTournamentService;

    @GetMapping
    public ResponseDataDTO<ClubTournamentHomeResponse> getTournamentHome(
            @PathVariable Long clubId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.getTournamentHome(clubId, requireUserKey(userContext)),
                "대회 홈 조회 성공"
        );
    }

    @GetMapping("/{tournamentRecordId}")
    public ResponseDataDTO<TournamentDetailResponse> getTournamentDetail(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.getTournamentDetail(clubId, tournamentRecordId, requireUserKey(userContext)),
                "대회 상세 조회 성공"
        );
    }

    @PostMapping
    public ResponseDataDTO<TournamentUpsertResponse> createTournament(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertTournamentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.createTournament(clubId, requireUserKey(userContext), request),
                "대회 생성 성공"
        );
    }

    @PutMapping("/{tournamentRecordId}")
    public ResponseDataDTO<TournamentUpsertResponse> updateTournament(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @Valid @RequestBody UpsertTournamentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.updateTournament(clubId, tournamentRecordId, requireUserKey(userContext), request),
                "대회 수정 성공"
        );
    }

    @PutMapping("/{tournamentRecordId}/cancel")
    public ResponseDataDTO<TournamentDetailResponse> cancelTournament(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @Valid @RequestBody CancelTournamentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.cancelTournament(clubId, tournamentRecordId, requireUserKey(userContext), request),
                "대회 취소 성공"
        );
    }

    @PostMapping("/{tournamentRecordId}/applications")
    public ResponseDataDTO<TournamentDetailResponse> applyToTournament(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @Valid @RequestBody(required = false) SubmitTournamentApplicationRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.applyToTournament(clubId, tournamentRecordId, requireUserKey(userContext), request),
                "대회 참가 신청 성공"
        );
    }

    @DeleteMapping("/{tournamentRecordId}/applications/me")
    public ResponseDataDTO<TournamentDetailResponse> cancelMyApplication(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubTournamentService.cancelMyApplication(clubId, tournamentRecordId, requireUserKey(userContext)),
                "대회 참가 신청 취소 성공"
        );
    }

    @PutMapping("/{tournamentRecordId}/applications/{tournamentApplicationId}/review")
    public ResponseDataDTO<TournamentDetailResponse> reviewApplication(
            @PathVariable Long clubId,
            @PathVariable Long tournamentRecordId,
            @PathVariable Long tournamentApplicationId,
            @Valid @RequestBody ReviewTournamentApplicationRequest request,
            UserContext userContext
    ) {
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

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }

}
