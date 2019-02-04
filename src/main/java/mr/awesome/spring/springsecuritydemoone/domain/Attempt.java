package mr.awesome.spring.springsecuritydemoone.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Attempt {
    private final int ALLOWED_FAILED_ATTEMPTS = 3;
    public static final Logger LOGGER = LoggerFactory.getLogger(Attempt.class);

    private final String user;
    private AtomicInteger attempts;
    private Instant attemptAfter;

    public Attempt(String user) {
        this.user = user;
        this.attempts = new AtomicInteger(0);
        this.attemptAfter = Instant.EPOCH;
    }

    public String getUser() {
        return user;
    }

    public Instant getAttemptAfter() {
        return attemptAfter;
    }

    public boolean canAttempt() {
        return Instant.now().isAfter(attemptAfter);
    }

    public void attemptFailed() {
        if (ALLOWED_FAILED_ATTEMPTS < attempts.incrementAndGet()) {
            if(Instant.now().isAfter(attemptAfter))
                attempts.set(0);
            long fiboTimeInMinutes = Attempt.getFiboTimeInMinutes(attempts.get() - ALLOWED_FAILED_ATTEMPTS);
            attemptAfter = Instant.now().plus(fiboTimeInMinutes, ChronoUnit.MINUTES);
            LOGGER.warn("{} needs to wait till {} ( in {} minutes) for next attempt!", user, attemptAfter, fiboTimeInMinutes);
        }
    }

    public static long getFiboTimeInMinutes(int count) {
        int first=1, second=2;
        if(count==1) return 1;
        if(count==2) return 2;
        for (int i = 0; i < count-2; i++) {
            int temp = first;
            first= second;
            second+=temp;
        }
        return second;
    }
}
