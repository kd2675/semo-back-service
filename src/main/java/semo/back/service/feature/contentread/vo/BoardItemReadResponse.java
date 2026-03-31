package semo.back.service.feature.contentread.vo;

public record BoardItemReadResponse(
        Long boardItemId,
        int readCount
) {
}
