package semo.back.service.database.pub.repository;

import semo.back.service.database.pub.entity.ClubNotice;

public record ClubNoticeBoardFeedRow(
        Long boardItemId,
        ClubNotice notice
) {
}
