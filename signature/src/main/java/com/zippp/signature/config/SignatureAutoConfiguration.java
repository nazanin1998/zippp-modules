package com.zippp.signature.config;

import com.zippp.signature.exception.SignatureKeyException;
import com.zippp.signature.service.JwtParser;
import com.zippp.signature.service.JwtSigner;
import com.zippp.signature.service.RsaKeyLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Spring Boot autoconfigure for the {@code signature} starter.
 *
 * <p>Target projects drop in a dependency on {@code com.zippp:signature} and
 * automatically receive:
 * <ul>
 *   <li>a {@link JwtSigner} (RSA private key, signs JWTs) — overridable,</li>
 *   <li>a {@link JwtParser} (RSA public key, verifies JWTs) — overridable.</li>
 * </ul>
 *
 * <h2>Activation rules</h2>
 * <ul>
 *   <li>{@code io.jsonwebtoken.Jwts} must be on the classpath (it is a direct
 *       dependency of this starter).</li>
 *   <li>Activation can be disabled with
 *       {@code zippp.signature.enabled=false} in {@code application.yml}.</li>
 *   <li>Every bean is {@link ConditionalOnMissingBean @ConditionalOnMissingBean}
 *       so the target project can override any of them by declaring its own
 *       bean of the same type.</li>
 * </ul>
 *
 * <p>Configuration lives under {@code zippp.signature.*}; see
 * {@link SignatureProperties} for the full property contract.
 */
@AutoConfiguration
@ConditionalOnClass(io.jsonwebtoken.Jwts.class)
@ConditionalOnProperty(prefix = "zippp.signature", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SignatureProperties.class)
public class SignatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PrivateKey signingPrivateKey(SignatureProperties properties) {
        String pem = readUtf8(properties.signingPrivateKey(),
                "zippp.signature.signing-private-key");
        return RsaKeyLoader.loadPrivateKey(pem);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicKey parsingPublicKey(SignatureProperties properties) {
        String pem = readUtf8(properties.parsingPublicKey(),
                "zippp.signature.parsing-public-key");
        return RsaKeyLoader.loadPublicKey(pem);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtSigner jwtSigner(PrivateKey signingPrivateKey) {
        return new JwtSigner(signingPrivateKey);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtParser jwtParser(PublicKey parsingPublicKey) {
        return new JwtParser(parsingPublicKey);
    }

    /**
     * Read the given resource as UTF-8. A missing/unreadable resource is a
     * configuration problem — surface it with the offending property name so
     * the failure message points the operator at the right key.
     */
    private static String readUtf8(Resource resource, String propertyName) {
        if (resource == null || !resource.exists()) {
            throw new SignatureKeyException(
                    "Configured " + propertyName + " does not point to an existing resource");
        }
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SignatureKeyException(
                    "Failed to read configured " + propertyName, e);
        }
    }
}
