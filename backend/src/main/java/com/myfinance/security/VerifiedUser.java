package com.myfinance.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class VerifiedUser {
    private String providerId;
    private String email;
    private String name;
    private String pictureUrl;
    private String provider;
}
