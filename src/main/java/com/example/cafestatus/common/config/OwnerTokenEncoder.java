package com.example.cafestatus.common.config;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class OwnerTokenEncoder {

    private static final int COST = 12;

    public String encode(String rawToken) {
        return BCrypt.withDefaults().hashToString(COST, rawToken.toCharArray());
    }

    public boolean matches(String rawToken, String encodedToken) {
        BCrypt.Result result = BCrypt.verifyer().verify(rawToken.toCharArray(), encodedToken);
        return result.verified;
    }
}
