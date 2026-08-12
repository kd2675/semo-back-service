package semo.back.service.common.jpa;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

@Component("semoDateTimeProvider")
@RequiredArgsConstructor
public class MonotonicDateTimeProvider implements DateTimeProvider {
    // CommonDateEntity still uses Jsr310JpaConverters.LocalDateTimeConverter,
    // which persists through java.util.Date and therefore keeps milliseconds.
    private static final long DATABASE_PRECISION_NANOS = 1_000_000L;

    private final Clock clock;
    private final AtomicReference<LocalDateTime> lastIssued = new AtomicReference<>();

    public LocalDateTime now() {
        LocalDateTime observed = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        return lastIssued.updateAndGet(previous -> previous == null || observed.isAfter(previous)
                ? observed
                : previous.plusNanos(DATABASE_PRECISION_NANOS));
    }

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(now());
    }
}
