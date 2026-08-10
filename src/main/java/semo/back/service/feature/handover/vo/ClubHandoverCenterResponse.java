package semo.back.service.feature.handover.vo;

import java.util.List;

public record ClubHandoverCenterResponse(
        Long clubId,
        String clubName,
        boolean fullAdmin,
        boolean canManage,
        Long viewerClubProfileId,
        ClubOperatingTermResponse activeTerm,
        ClubOperatingTermResponse nextTerm,
        ClubOperatingTermResponse selectedTerm,
        List<ClubOperatingTermResponse> terms,
        List<ClubExecutiveAssignmentResponse> executiveAssignments,
        List<ClubHandoverNoteResponse> handoverNotes,
        List<ClubTermCarryoverItemResponse> carryoverItems,
        HandoverQueueSummaryResponse queueSummary,
        List<HandoverQueueItemResponse> queueItems,
        ClubTermMetricsResponse activeTermMetrics,
        List<HandoverMemberOptionResponse> memberOptions,
        List<HandoverPositionOptionResponse> positionOptions
) {
}
