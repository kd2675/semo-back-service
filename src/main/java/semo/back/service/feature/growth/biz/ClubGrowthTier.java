package semo.back.service.feature.growth.biz;

import java.util.Arrays;

public enum ClubGrowthTier {
    RAW(0, "원석"),
    IRON(1, "아이언"),
    BRONZE(2, "브론즈"),
    SILVER(3, "실버"),
    GOLD(4, "골드"),
    PLATINUM(5, "플래티넘"),
    DIAMOND(6, "다이아");

    private final int level;
    private final String label;

    ClubGrowthTier(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int level() {
        return level;
    }

    public String label() {
        return label;
    }

    public static ClubGrowthTier fromLevel(int level) {
        return Arrays.stream(values())
                .filter(tier -> tier.level == level)
                .findFirst()
                .orElse(level < 0 ? RAW : DIAMOND);
    }
}
