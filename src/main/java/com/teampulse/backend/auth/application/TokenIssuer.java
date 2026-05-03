package com.teampulse.backend.auth.application;

import com.teampulse.backend.auth.domain.AuthUser;
import com.teampulse.backend.auth.dto.JwtInfo;

public interface TokenIssuer {

    JwtInfo issue(AuthUser user);
}
