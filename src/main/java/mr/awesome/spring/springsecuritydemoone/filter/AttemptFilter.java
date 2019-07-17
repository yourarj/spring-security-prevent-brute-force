package mr.awesome.spring.springsecuritydemoone.filter;

import mr.awesome.spring.springsecuritydemoone.service.LoginAttemptService;
import mr.awesome.spring.springsecuritydemoone.service.LoginAttemptServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

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
                AsyncContext asyncContext = request.startAsync();
                long l =Duration.between(Instant.now(),loginAttemptService.cantAttemptAfter(userKey)).toMillis();
                l=l>0?l:1;
                LOGGER.warn("Holding response for {} milliseconds", l);
                asyncContext.setTimeout(-1);
                loginAttemptService.saveForLater(userKey,new LoginAttemptServiceImpl.ChainNAsyncContext(chain,asyncContext));
                asyncContext.addListener(new MyAsyncListener());
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    class MyAsyncListener implements AsyncListener{
        //private final FilterChain chain;

        MyAsyncListener(/*FilterChain chain*/) {
            //this.chain = chain;
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            LOGGER.debug("On AsyncRequest Complete");
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            LOGGER.debug("On AsyncRequest Timeout");
/*
            try {
                chain.doFilter(event.getSuppliedRequest(), event.getSuppliedResponse());
            }catch (ServletException se){
                throw new IOException(se);
            }
*/
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            LOGGER.debug("On AsyncRequest Error");
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            LOGGER.debug("On AsyncRequest Error");
        }
    }
}
