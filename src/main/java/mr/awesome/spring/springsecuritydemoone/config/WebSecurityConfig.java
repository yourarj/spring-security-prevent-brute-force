package mr.awesome.spring.springsecuritydemoone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    protected UserDetailsService userDetailsService(){
        UserDetails rj = User
                .builder()
                .username("rj")
                //pass
                .password("{pbkdf2}6a2ac7fe5ef21e837f2df2118721ee7d70c56bbc6a64bc0881a7368b1444dca0e8c50087a8b12836")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(rj);
    }
}
