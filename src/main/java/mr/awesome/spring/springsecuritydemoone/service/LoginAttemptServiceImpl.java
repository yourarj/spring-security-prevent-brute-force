package mr.awesome.spring.springsecuritydemoone.service;

import mr.awesome.spring.springsecuritydemoone.domain.Attempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptServiceImpl implements LoginAttemptService, ApplicationListener<AbstractAuthenticationEvent> {
    private final ConcurrentHashMap<String, Attempt> attemptMap = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);

    @Override
    public boolean canAttemptNow(String user) {
        Attempt attempt = attemptMap.get(user);
        if(null!=attempt)
            return attempt.canAttempt();
        else
            return true;
    }

    @Override
    public Instant cantAttemptAfter(String user) {
        Attempt attempt = attemptMap.get(user);
        if(attempt!=null){
            return attempt.getAttemptAfter();
        }else{
            return Instant.ofEpochMilli(0);
        }
    }

    @Override
    public void registerFailedAttempt(String user) {
        LOGGER.warn("Registering failed event for user {}", user);
        Attempt attempt = attemptMap.get(user);
        if(attempt==null){
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
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        String remoteAddress = "";
        String userKey = event.getAuthentication().getName();
        if(event instanceof AuthenticationFailureBadCredentialsEvent){
            Object source = event.getSource();
            if(source instanceof UsernamePasswordAuthenticationToken){
                Object details = ((UsernamePasswordAuthenticationToken) source).getDetails();
                if(details instanceof WebAuthenticationDetails){
                    remoteAddress = ((WebAuthenticationDetails) details).getRemoteAddress();
                }
            }
            userKey+=remoteAddress;
            registerFailedAttempt(userKey);
        } else if( event instanceof AuthenticationSuccessEvent){
            clearHistory(userKey);
        }
    }
}
