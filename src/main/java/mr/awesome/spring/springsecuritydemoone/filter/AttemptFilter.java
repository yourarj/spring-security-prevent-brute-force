package mr.awesome.spring.springsecuritydemoone.filter;

import mr.awesome.spring.springsecuritydemoone.service.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AttemptFilter implements Filter {
    public static final Logger LOGGER = LoggerFactory.getLogger(AttemptFilter.class);
    private final LoginAttemptService loginAttemptService;

    public AttemptFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (null != username) {
            String userKey = username+request.getRemoteAddr();
            if (loginAttemptService.canAttemptNow(userKey)) {
                chain.doFilter(request, response);
            } else {
                ((HttpServletResponse) response).setHeader("Content-Type", "application/json");
                ((HttpServletResponse) response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                Instant instant = loginAttemptService.cantAttemptAfter(userKey);
                Duration between = Duration.between(Instant.now(), instant);
                byte[] message = String.format("{\"message\":\"You need to wait till %s (%d minutes, %d seconds) for next login attempt\"}", instant, between.getSeconds()/60, between.getSeconds()%60).getBytes();
                response.getOutputStream().write(message);
            }
        }else {
            chain.doFilter(request, response);
        }
    }
}
