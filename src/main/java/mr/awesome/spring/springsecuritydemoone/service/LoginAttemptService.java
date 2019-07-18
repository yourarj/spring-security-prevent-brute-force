package mr.awesome.spring.springsecuritydemoone.service;

import javax.servlet.AsyncContext;
import java.time.Instant;

public interface LoginAttemptService {
    boolean canAttemptNow(String user);
    Instant cantAttemptAfter(String user);
    void registerFailedAttempt(String user);
    void clearHistory(String name);
    void addToWaitingQueue(String name, AsyncContext chainNAsyncContext);
}
