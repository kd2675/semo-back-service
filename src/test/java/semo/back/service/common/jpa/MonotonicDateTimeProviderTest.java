package semo.back.service.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class MonotonicDateTimeProviderTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-11T00:00:00Z");

    @Test
    void now_firstCall_usesClockAtDatabasePrecision() {
        MonotonicDateTimeProvider provider = provider();

        assertThat(provider.now()).isEqualTo(LocalDateTime.of(2026, 8, 11, 9, 0));
    }

    @Test
    void now_sameClockInstant_advancesByOneMillisecond() {
        MonotonicDateTimeProvider provider = provider();
        LocalDateTime first = provider.now();

        assertThat(provider.now()).isEqualTo(first.plusNanos(1_000_000));
    }

    private MonotonicDateTimeProvider provider() {
        return new MonotonicDateTimeProvider(Clock.fixed(FIXED_INSTANT, SEOUL));
    }
}
