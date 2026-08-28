package com.zippp.signature.service;

import com.zippp.signature.exception.SignatureKeyException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Internal helper for loading RSA keys supplied via configuration.
 *
 * <p>Configuration accepts three shapes — all normalized to a DER byte array:
 * <ul>
 *   <li>PEM with markers: {@code -----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----}</li>
 *   <li>PEM without markers: stripped Base64 body only</li>
 *   <li>Raw Base64 of the DER bytes</li>
 * </ul>
 * <p>This is intentionally permissive so the target project's
 * {@code application.yml} can use a pipe-scalar PEM block, a single-line
 * Base64 string, or anything in between.
 */
public final class RsaKeyLoader {

    /** Matches the BEGIN/END marker lines of any PEM block. */
    private static final Pattern PEM_MARKERS = Pattern.compile(
            "-----BEGIN [^-]+-----|-----END [^-]+-----");

    private RsaKeyLoader() {
    }

    /**
     * Load a PKCS#8 RSA private key from a PEM/DER string.
     *
     * @throws SignatureKeyException if the string is blank or the key cannot be parsed.
     */
    public static PrivateKey loadPrivateKey(String pem) {
        byte[] der = pemToDer(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SignatureKeyException(
                    "Configured zippp.signature.signing-private-key is not a valid RSA private key", e);
        }
    }

    /**
     * Load an X.509 (SubjectPublicKeyInfo) RSA public key from a PEM/DER string.
     *
     * @throws SignatureKeyException if the string is blank or the key cannot be parsed.
     */
    public static PublicKey loadPublicKey(String pem) {
        byte[] der = pemToDer(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(new X509EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SignatureKeyException(
                    "Configured zippp.signature.parsing-public-key is not a valid RSA public key", e);
        }
    }

    /**
     * Strip PEM markers (if any), decode Base64, return DER bytes.
     *
     * <p>Throws {@link SignatureKeyException} for blank input — that's almost
     * always a configuration typo rather than a runtime decision, and silently
     * returning empty bytes would later produce a confusing
     * {@code InvalidKeySpecException}.
     */
    private static byte[] pemToDer(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new SignatureKeyException(
                    "Key material is blank; check zippp.signature.signing-private-key / parsing-public-key");
        }
        String stripped = PEM_MARKERS.matcher(pem).replaceAll("").replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(stripped);
        } catch (IllegalArgumentException e) {
            throw new SignatureKeyException(
                    "Key material is not valid Base64; expected PEM or Base64-encoded DER", e);
        }
    }
}