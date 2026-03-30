package com.myfinance.security;

public interface TokenVerifier {

    /** The SSO provider name, e.g. "google", "microsoft", "github". */
    String getProvider();

    /** Verify the external SSO token and return verified user info. */
    VerifiedUser verify(String token);
}
