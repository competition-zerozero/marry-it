package com.zerozero.marryit.auth.config;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/login", "/error", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()));

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2.successHandler(oauth2LoginSuccessHandler));
        }

        return http.build();
    }
}
