package semo.back.service.feature.bracket.biz;

import java.util.ArrayList;
import java.util.List;

final class BracketSeedPlacement {
    private BracketSeedPlacement() {
    }

    static List<Integer> orderSeeds(int participantCount) {
        if (participantCount < 1) {
            return List.of();
        }

        int bracketSize = 1;
        while (bracketSize < participantCount) {
            bracketSize *= 2;
        }
        if (bracketSize == 1) {
            return List.of(1);
        }

        List<Integer> placement = new ArrayList<>(List.of(1, 2));
        for (int size = 4; size <= bracketSize; size *= 2) {
            List<Integer> expanded = new ArrayList<>(size);
            for (Integer seed : placement) {
                expanded.add(seed);
                expanded.add(size + 1 - seed);
            }
            placement = expanded;
        }
        return List.copyOf(placement);
    }
}
