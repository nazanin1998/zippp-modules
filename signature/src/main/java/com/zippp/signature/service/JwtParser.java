package com.zippp.signature.service;

import com.zippp.signature.dto.ParsedJwtDto;
import com.zippp.signature.exception.SignatureParseException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;
import java.util.Map;


public final class JwtParser {

    private final PublicKey publicKey;

    public JwtParser(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public Map<String, Object> parseJwt(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new SignatureParseException("jwt must not be null or blank");
        }
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new SignatureParseException("JWT expired", e);
        } catch (JwtException e) {
            throw new SignatureParseException("JWT verification failed", e);
        }
    }
}