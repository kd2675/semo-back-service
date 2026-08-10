package semo.back.service.feature.bracket.biz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BracketSeedPlacementTest {
    @Test
    void orderSeeds_eightPlayerBracket_separatesTopSeeds() {
        assertThat(BracketSeedPlacement.orderSeeds(8))
                .containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }

    @Test
    void orderSeeds_sixPlayerBracket_placesByesAgainstTopSeeds() {
        assertThat(BracketSeedPlacement.orderSeeds(6))
                .containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }
}
