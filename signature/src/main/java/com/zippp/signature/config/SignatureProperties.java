package com.zippp.signature.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for the {@code signature} starter, bound to the
 * {@code zippp.signature.*} namespace in the target project's
 * {@code application.yml}.
 *
 * <p>Activated by
 * {@link SignatureAutoConfiguration @SignatureAutoConfiguration} via
 * {@link org.springframework.boot.context.properties.EnableConfigurationProperties}.
 * Both properties are validated at startup; missing or unresolvable resources
 * cause a {@code ConfigurationPropertiesBindException} on context refresh —
 * the app fails fast rather than signing with a {@code null} key.
 *
 * <h2>Why {@link Resource}?</h2>
 * Both keys are accepted as Spring {@link Resource} locations
 * (e.g. {@code classpath:private.pem}, {@code file:/etc/zippp/private.pem}).
 * This keeps PEM material out of {@code application.yml} and lets ops mount
 * keys as files in containers — preferable to inlining secrets in config.
 *
 * <h2>Why two keys?</h2>
 * The starter uses {@code RS256} (asymmetric RSA). The signing service uses
 * {@link #signingPrivateKey()} to mint tokens; the parsing service uses
 * {@link #parsingPublicKey()} to verify them. Holding the signing key private
 * and distributing only the parsing key lets any party verify a token without
 * being able to forge one — even the verification service cannot mint a token.
 *
 * <h2>Example</h2>
 * <pre>
 * zippp:
 *   signature:
 *     enabled: true
 *     signing-private-key: classpath:keys/private.pem
 *     parsing-public-key:  classpath:keys/public.pem
 * </pre>
 *
 * <p>Either file may be raw PEM (with the {@code -----BEGIN/-----END} markers)
 * or Base64-encoded DER. The contents are read once at startup.
 *
 * @param signingPrivateKey RSA private key, loaded as a Spring {@link Resource}.
 *                          Used by {@link com.zippp.signature.service.JwtSigner}.
 *                          Must be non-null and resolvable.
 * @param parsingPublicKey  RSA public key, loaded as a Spring {@link Resource}.
 *                          Used by {@link com.zippp.signature.service.JwtParser}.
 *                          Must be non-null and resolvable.
 */
@ConfigurationProperties(prefix = "zippp.signature")
@Validated
public record SignatureProperties(

        @NotNull
        Resource signingPrivateKey,

        @NotNull
        Resource parsingPublicKey

) {
}
