package com.dealmanager.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * DEMO-ONLY authentication: HTTP Basic with two in-memory users so roles can be shown.
 * A bank would sit behind its identity provider (OIDC/SAML) with roles from entitlements.
 * CSRF is off because the API is stateless (no session cookie) and only takes Basic auth.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                // Plain 401 without WWW-Authenticate, so browsers don't pop a login dialog.
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, ex) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("rm.user").password("{noop}demo").roles("RM").build(),
                User.withUsername("committee.user").password("{noop}demo").roles("PRICING_COMMITTEE").build());
    }
}
