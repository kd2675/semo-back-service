package semo.back.service.feature.todo.vo;

public record TodoDecisionOptionResponse(
        Long decisionRecordId,
        String title,
        String statusCode,
        String confirmedAtLabel
) {
}
