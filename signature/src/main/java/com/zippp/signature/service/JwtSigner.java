package com.zippp.signature.service;

import io.jsonwebtoken.Jwts;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class JwtSigner {

    private final PrivateKey privateKey;

    public JwtSigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String sign(Map<String, Object> map, Duration expiration) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("value must not be null");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(map)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiration.getSeconds())))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}