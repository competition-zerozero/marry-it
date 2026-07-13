package com.zerozero.marryit.auth.config;

import com.zerozero.marryit.auth.oauth.InviteTokenCaptureFilter;
import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!mcp")
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            InviteTokenCaptureFilter inviteTokenCaptureFilter,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            @Value("${app.frontend-url:/}") String frontendUrl
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/login", "/error", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/workspaces/invitations/**").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessUrl(frontendUrl))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()))
                .addFilterBefore(inviteTokenCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class);

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureUrl(frontendUrl + "/?loginError=true")
            );
        }

        return http.build();
    }
}
