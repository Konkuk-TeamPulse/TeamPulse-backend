package com.teampulse.backend.auth.infrastructure;

import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.domain.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DemoAccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenVerifier accessTokenVerifier;

    public DemoAccessTokenAuthenticationFilter(AccessTokenVerifier accessTokenVerifier) {
        this.accessTokenVerifier = accessTokenVerifier;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authorization = request.getHeader("Authorization");
        var user = authorization == null
                ? Optional.<AuthUser>empty()
                : accessTokenVerifier.findUserByAccessToken(authorization.trim());
        if (user.isPresent()) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    user.get(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
