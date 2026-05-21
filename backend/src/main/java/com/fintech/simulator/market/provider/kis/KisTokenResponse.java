package com.fintech.simulator.market.provider.kis;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KIS /oauth2/tokenP 응답.
 * <pre>
 * {"access_token":"...","token_type":"Bearer","expires_in":86400}
 * </pre>
 */
public record KisTokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("expires_in")    Long   expiresInSeconds
) {}
