package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record UpdateClubFeaturesRequest(
        List<String> enabledFeatureKeys
) {
}
