package semo.back.service.feature.handover.act;

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
import semo.back.service.feature.handover.biz.ClubHandoverService;
import semo.back.service.feature.handover.vo.ClubExecutiveAssignmentResponse;
import semo.back.service.feature.handover.vo.ClubHandoverCenterResponse;
import semo.back.service.feature.handover.vo.ClubHandoverNoteResponse;
import semo.back.service.feature.handover.vo.ClubOperatingTermResponse;
import semo.back.service.feature.handover.vo.ClubTermCarryoverItemResponse;
import semo.back.service.feature.handover.vo.CreateOperatingTermRequest;
import semo.back.service.feature.handover.vo.UpdateOperatingTermRequest;
import semo.back.service.feature.handover.vo.UpsertExecutiveAssignmentRequest;
import semo.back.service.feature.handover.vo.UpsertHandoverNoteRequest;
import web.common.core.response.base.dto.ResponseDataDTO;

@RequirePrincipalRole
@RestController
@RequestMapping("/api/semo/v1/clubs/{clubId}/admin/more/handover")
@RequiredArgsConstructor
public class ClubHandoverController {
    private final ClubHandoverService clubHandoverService;

    @GetMapping
    public ResponseDataDTO<ClubHandoverCenterResponse> getCenter(
            @PathVariable Long clubId,
            @RequestParam(required = false) Long termId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.getCenter(clubId, requireUserKey(userContext), termId),
                "인수인계 센터 조회 성공"
        );
    }

    @PostMapping("/terms")
    public ResponseDataDTO<ClubOperatingTermResponse> createTerm(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateOperatingTermRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.createTerm(clubId, requireUserKey(userContext), request),
                "운영 임기 생성 성공"
        );
    }

    @PutMapping("/terms/{termId}")
    public ResponseDataDTO<ClubOperatingTermResponse> updateTerm(
            @PathVariable Long clubId,
            @PathVariable Long termId,
            @Valid @RequestBody UpdateOperatingTermRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.updateTerm(clubId, termId, requireUserKey(userContext), request),
                "운영 임기 수정 성공"
        );
    }

    @PutMapping("/terms/{termId}/activate")
    public ResponseDataDTO<ClubOperatingTermResponse> activateTerm(
            @PathVariable Long clubId,
            @PathVariable Long termId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.activateTerm(clubId, termId, requireUserKey(userContext)),
                "운영 임기 시작 성공"
        );
    }

    @PutMapping("/terms/{termId}/close")
    public ResponseDataDTO<ClubOperatingTermResponse> closeTerm(
            @PathVariable Long clubId,
            @PathVariable Long termId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.closeTerm(clubId, termId, requireUserKey(userContext)),
                "운영 임기 종료 성공"
        );
    }

    @PostMapping("/terms/{termId}/executives")
    public ResponseDataDTO<ClubExecutiveAssignmentResponse> upsertExecutiveAssignment(
            @PathVariable Long clubId,
            @PathVariable Long termId,
            @Valid @RequestBody UpsertExecutiveAssignmentRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.upsertExecutiveAssignment(
                        clubId,
                        termId,
                        requireUserKey(userContext),
                        request
                ),
                "집행부 구성 저장 성공"
        );
    }

    @DeleteMapping("/executives/{assignmentId}")
    public ResponseDataDTO<Boolean> deleteExecutiveAssignment(
            @PathVariable Long clubId,
            @PathVariable Long assignmentId,
            UserContext userContext
    ) {
        clubHandoverService.deleteExecutiveAssignment(clubId, assignmentId, requireUserKey(userContext));
        return ResponseDataDTO.of(true, "집행부 배정 삭제 성공");
    }

    @PostMapping("/notes")
    public ResponseDataDTO<ClubHandoverNoteResponse> createHandoverNote(
            @PathVariable Long clubId,
            @Valid @RequestBody UpsertHandoverNoteRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.createHandoverNote(clubId, requireUserKey(userContext), request),
                "인수인계 메모 생성 성공"
        );
    }

    @PutMapping("/notes/{noteId}")
    public ResponseDataDTO<ClubHandoverNoteResponse> updateHandoverNote(
            @PathVariable Long clubId,
            @PathVariable Long noteId,
            @Valid @RequestBody UpsertHandoverNoteRequest request,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.updateHandoverNote(
                        clubId,
                        noteId,
                        requireUserKey(userContext),
                        request
                ),
                "인수인계 메모 수정 성공"
        );
    }

    @PutMapping("/notes/{noteId}/acknowledge")
    public ResponseDataDTO<ClubHandoverNoteResponse> acknowledgeHandoverNote(
            @PathVariable Long clubId,
            @PathVariable Long noteId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.acknowledgeHandoverNote(clubId, noteId, requireUserKey(userContext)),
                "인수인계 메모 확인 성공"
        );
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseDataDTO<Boolean> deleteHandoverNote(
            @PathVariable Long clubId,
            @PathVariable Long noteId,
            UserContext userContext
    ) {
        clubHandoverService.deleteHandoverNote(clubId, noteId, requireUserKey(userContext));
        return ResponseDataDTO.of(true, "인수인계 메모 삭제 성공");
    }

    @PutMapping("/carryovers/{carryoverItemId}/resolve")
    public ResponseDataDTO<ClubTermCarryoverItemResponse> resolveCarryover(
            @PathVariable Long clubId,
            @PathVariable Long carryoverItemId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.updateCarryoverStatus(
                        clubId,
                        carryoverItemId,
                        requireUserKey(userContext),
                        true
                ),
                "이관 항목 완료 처리 성공"
        );
    }

    @PutMapping("/carryovers/{carryoverItemId}/reopen")
    public ResponseDataDTO<ClubTermCarryoverItemResponse> reopenCarryover(
            @PathVariable Long clubId,
            @PathVariable Long carryoverItemId,
            UserContext userContext
    ) {
        return ResponseDataDTO.of(
                clubHandoverService.updateCarryoverStatus(
                        clubId,
                        carryoverItemId,
                        requireUserKey(userContext),
                        false
                ),
                "이관 항목 미완료 전환 성공"
        );
    }

    private String requireUserKey(UserContext userContext) {
        if (userContext == null || !StringUtils.hasText(userContext.getUserKey())) {
            throw new SemoException.UnauthorizedException("Login required");
        }
        return userContext.getUserKey();
    }
}
