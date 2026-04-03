package semo.back.service.feature.todo.vo;

public record TodoMemberOptionResponse(
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode
) {
}
