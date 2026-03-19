package com.qa.testmanagement.config;

import com.qa.testmanagement.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Public pages
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/webjars/**").permitAll()
                        // Admin only
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Tester and Admin can create/edit/delete
                        .requestMatchers("/testcases/new", "/testcases/edit/**", "/testcases/delete/**")
                        .hasAnyRole("ADMIN", "TESTER")
                        .requestMatchers("/testcases/execute/**", "/executions/**").hasAnyRole("ADMIN", "TESTER")
                        .requestMatchers("/upload").hasAnyRole("ADMIN", "TESTER")
                        // All authenticated users can view
                        .requestMatchers("/dashboard", "/testcases/list", "/executions/history/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretKey123")
                        .tokenValiditySeconds(86400) // 24 hours
                )
                .userDetailsService(userDetailsService); // Add this line

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/dashboard");
        };
    }

    // Remove the configureGlobal method - it's causing circular reference
    // The userDetailsService is already set in the filterChain
}