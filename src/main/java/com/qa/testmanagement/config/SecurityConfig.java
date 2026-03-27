package com.qa.testmanagement.config;

import com.qa.testmanagement.service.CustomUserDetailsService;
import com.qa.testmanagement.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ActiveUserService activeUserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Public pages
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/webjars/**").permitAll()
                        // API endpoints - PUBLIC (or you can add API key filter later)
                        .requestMatchers("/api/**").permitAll()
                        // Swagger UI
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        // Admin only
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Tester and Admin can create/edit/delete
                        .requestMatchers("/testcases/new", "/testcases/edit/**", "/testcases/delete/**")
                        .hasAnyRole("ADMIN", "TESTER")
                        .requestMatchers("/testcases/execute/**", "/executions/**").hasAnyRole("ADMIN", "TESTER")
                        .requestMatchers("/upload").hasAnyRole("ADMIN", "TESTER")
                        .requestMatchers("/reports/**").hasAnyRole("ADMIN", "TESTER")
                        // All authenticated users can view
                        .requestMatchers("/dashboard", "/testcases/list", "/executions/history/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .addLogoutHandler(customLogoutHandler())
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretKey123")
                        .tokenValiditySeconds(86400))
                .userDetailsService(userDetailsService)
                .sessionManagement(session -> session
                        .maximumSessions(100) // Maximum concurrent sessions per user
                        .expiredUrl("/login?expired"));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            HttpSession session = request.getSession(true);
            String sessionId = session.getId();

            // Track active user
            activeUserService.userLoggedIn(username, sessionId);
            session.setAttribute("username", username);

            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public LogoutHandler customLogoutHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    String sessionId = session.getId();
                    activeUserService.userLoggedOut(sessionId);
                }
            }
        };
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}