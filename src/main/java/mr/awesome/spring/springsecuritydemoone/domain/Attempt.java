package mr.awesome.spring.springsecuritydemoone.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Attempt {
    public final static int ALLOWED_FAILED_ATTEMPTS = 3;
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

    public int getFailedLoginAttempts(){
        return attempts.get();
    }

    public boolean canAttempt() {
        return Instant.now().isAfter(attemptAfter);
    }

    public void attemptFailed() {
        if (ALLOWED_FAILED_ATTEMPTS <= attempts.incrementAndGet()) {
            long timeInSeconds = Attempt.getFibonacciTimeInSeconds(attempts.get() - ALLOWED_FAILED_ATTEMPTS + 1);
            attemptAfter = Instant.now().plus(timeInSeconds, ChronoUnit.SECONDS);
            LOGGER.warn("{} needs to wait till {} ( in {} seconds) for next attempt!", user, attemptAfter, timeInSeconds);
        }
    }

    public static long getFibonacciTimeInSeconds(int count) {
        LOGGER.debug("Get fibonacci time for count {}", count);
        int first = 1, second = 2;
        if (count == 1) return 10;
        if (count == 2) return 20;
        for (int i = 0; i < count - 2; i++) {
            int temp = first;
            first = second;
            second += temp;
        }
        return second * 10;
    }
}
