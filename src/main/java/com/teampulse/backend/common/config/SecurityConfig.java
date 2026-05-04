package com.teampulse.backend.common.config;

import com.teampulse.backend.auth.infrastructure.DemoAccessTokenAuthenticationFilter;
import com.teampulse.backend.common.api.SpecResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DemoAccessTokenAuthenticationFilter demoAccessTokenAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.security.public-api-docs:true}") boolean publicApiDocs,
            @Value("${app.security.public-roadmap:false}") boolean publicRoadmap
    ) throws Exception {
        var publicMatchers = new ArrayList<>(List.of(
                "/api/health",
                "/api/demo/**",
                "/api/mobile/**",
                "/api/auth/**"
        ));
        if (publicApiDocs) {
            publicMatchers.add("/v3/api-docs/**");
        }
        if (publicRoadmap) {
            publicMatchers.add("/api/roadmap");
        }

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeAuthenticationRequired(response, objectMapper))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeAccessDenied(response, objectMapper)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicMatchers.toArray(String[]::new))
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invitations/*")
                        .permitAll()
                        .requestMatchers(
                                "/api/users/me",
                                "/api/account",
                                "/api/account/**",
                                "/api/projects/**",
                                "/api/tasks/**",
                                "/api/meetings/**",
                                "/api/reports/**",
                                "/api/invitations/**"
                        )
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(demoAccessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeAuthenticationRequired(HttpServletResponse response, ObjectMapper objectMapper) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), SpecResponse.fail(3001, "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.", null));
    }

    private void writeAccessDenied(HttpServletResponse response, ObjectMapper objectMapper) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), SpecResponse.fail(3008, "접근 권한이 없습니다.", null));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

