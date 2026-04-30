package com.teampulse.backend.auth.infrastructure;

import com.teampulse.backend.auth.application.AccessTokenVerifier;
import com.teampulse.backend.auth.application.RefreshTokenRegistry;
import com.teampulse.backend.auth.application.TokenIssuer;
import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.dto.JwtInfo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DemoTokenIssuer implements TokenIssuer, RefreshTokenRegistry, AccessTokenVerifier {

    private final Set<String> activeRefreshTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> activeAccessTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, String> accessTokenByRefreshToken = new ConcurrentHashMap<>();
    private final Map<String, AuthUser> userByAccessToken = new ConcurrentHashMap<>();

    @Override
    public JwtInfo issue(AuthUser user) {
        var tokenSeed = user.id() + ":" + user.email();
        var token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tokenSeed.getBytes(StandardCharsets.UTF_8));
        var accessToken = "Bearer demo-access-" + token;
        var refreshToken = "Bearer demo-refresh-" + token;
        activeAccessTokens.add(accessToken);
        activeRefreshTokens.add(refreshToken);
        accessTokenByRefreshToken.put(refreshToken, accessToken);
        userByAccessToken.put(accessToken, user);
        return new JwtInfo(accessToken, refreshToken);
    }

    @Override
    public boolean isActive(String refreshToken) {
        return activeRefreshTokens.contains(refreshToken);
    }

    @Override
    public void revoke(String refreshToken) {
        activeRefreshTokens.remove(refreshToken);
        var accessToken = accessTokenByRefreshToken.remove(refreshToken);
        if (accessToken != null) {
            activeAccessTokens.remove(accessToken);
            userByAccessToken.remove(accessToken);
        }
    }

    @Override
    public boolean isActiveAccessToken(String accessToken) {
        return activeAccessTokens.contains(accessToken);
    }

    @Override
    public Optional<AuthUser> findUserByAccessToken(String accessToken) {
        if (!isActiveAccessToken(accessToken)) {
            return Optional.empty();
        }
        return Optional.ofNullable(userByAccessToken.get(accessToken));
    }
}
