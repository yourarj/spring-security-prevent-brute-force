package mr.awesome.spring.springsecuritydemoone.service;

import mr.awesome.spring.springsecuritydemoone.domain.Attempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import javax.servlet.AsyncContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class LoginAttemptServiceImpl implements LoginAttemptService, ApplicationListener<AbstractAuthenticationEvent> {
    public static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);
    private final ConcurrentHashMap<String, Attempt> attemptMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ChainNAsyncContext>> chainNAsuncContextMap = new ConcurrentHashMap<>();

    public static class ChainNAsyncContext {
        private final FilterChain filterChain;
        private final AsyncContext asyncContext;


        public ChainNAsyncContext(FilterChain filterChain, AsyncContext asyncContext) {
            this.filterChain = filterChain;
            this.asyncContext = asyncContext;
        }

        public AsyncContext getAsyncContext() {
            return asyncContext;
        }

        public FilterChain getFilterChain() {
            return filterChain;
        }

    }

    @Override
    public boolean canAttemptNow(String user) {
        Attempt attempt = attemptMap.get(user);
        if (null != attempt)
            return attempt.canAttempt();
        else
            return true;
    }

    @Override
    public Instant cantAttemptAfter(String user) {
        Attempt attempt = attemptMap.get(user);
        if (attempt != null) {
            return attempt.getAttemptAfter();
        } else {
            return Instant.ofEpochMilli(0);
        }
    }

    @Override
    public void registerFailedAttempt(String user) {
        LOGGER.warn("Registering failed event for user {}", user);
        Attempt attempt = attemptMap.get(user);
        if (attempt == null) {
            attempt = new Attempt(user);
            attemptMap.put(user, attempt);
        }
        attempt.attemptFailed();
    }

    @Override
    public void clearHistory(String name) {
        LOGGER.info("Login successful clearing records for user {}!", name);
        attemptMap.remove(name);
    }

    @Override
    public void saveForLater(String name, ChainNAsyncContext chainNAsyncContext) {
        chainNAsuncContextMap.computeIfAbsent(name, k -> new ArrayList<>());
        chainNAsuncContextMap.get(name).add(chainNAsyncContext);
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        String remoteAddress = "";
        String userKey = event.getAuthentication().getName();
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            Object source = event.getSource();
            if (source instanceof UsernamePasswordAuthenticationToken) {
                Object details = ((UsernamePasswordAuthenticationToken) source).getDetails();
                if (details instanceof WebAuthenticationDetails) {
                    remoteAddress = ((WebAuthenticationDetails) details).getRemoteAddress();
                }
            }
            userKey += remoteAddress;
            registerFailedAttempt(userKey);
        } else if (event instanceof AuthenticationSuccessEvent) {
            clearHistory(userKey);
        }
    }

    @Scheduled(fixedDelay = 500)
    public void scanner() {
        final Instant now = Instant.now();
        final List<String> badUsers = attemptMap.values().stream()
                .filter(attempt -> attempt.getAttemptAfter().isBefore(now) && attempt.getFailedLoginAttempts() >= Attempt.ALLOWED_FAILED_ATTEMPTS && null != chainNAsuncContextMap.get(attempt.getUser()))
                .map(Attempt::getUser).collect(Collectors.toList());
        if (badUsers.size() > 0)
            LOGGER.debug("Found {} users for processing", badUsers.size());
        for (String badUser : badUsers) {
            final List<ChainNAsyncContext> chainNAsyncContexts = chainNAsuncContextMap.get(badUser);
            if (null != chainNAsyncContexts)
                for (ChainNAsyncContext chainNAsyncContext : chainNAsyncContexts) {
                    try {
                        chainNAsyncContext.filterChain.doFilter(chainNAsyncContext.getAsyncContext().getRequest(), chainNAsyncContext.getAsyncContext().getResponse());
                        chainNAsyncContext.asyncContext.complete();
                    } catch (IOException | ServletException e) {
                        LOGGER.error("Error occurred while doFilter in scheduler");
                    }
                }
        }
        badUsers.forEach(badUser -> chainNAsuncContextMap.get(badUser).clear());
    }
}
