package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Support de test : signe des jetons de test (RS256) avec une clé privée RSA de test, dont la clé publique
 * est déclarée dans {@code src/test/resources/application.properties} ({@code quarkus.oidc.public-key}).
 * Le jeton est donc validé par {@code quarkus-oidc} (signature, iss, aud, exp) comme en production.
 * <p>
 * ATTENTION : paire de clés générée uniquement pour les tests, sans aucune valeur en dehors d'eux.
 */
final class JwtTestSupport {

    /** Clé privée RSA 2048 de test (PKCS#8, Base64). */
    private static final String TEST_PRIVATE_KEY_PKCS8 = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDD8AQuBOn4uzMtU/vBIaxL2nB6Jp7+6f5urozDMnyAxvO7a9TFQ6ImvxQnIfzEiG4hfIlCffGkRT09YGDlEQMNKXWEu87dYkZBt6hfL2WIjZ6BM62tyvorhL7ETzjBJl50egBzklrwihSPfwsBcM7f7DUl6O6rAyaLCkHy8ZuOBsyMtlpz2fTf5h4ti514Jk9KVMjvQvphuuGxxHOPwx40ojFNkM6aZTqkfXURrfiHlamHFtVBQWdpY3X0VbyjoCMmEVIbvTixdq6kq4h1rcTOU3+LKMrxSIZq50cVMwFvyUFv3JNZxunfXzmv3jAT1LkBC2MtiQCItaW+WfXywCSNAgMBAAECggEAAtwhalhBKNzSoUNQjNI/29cA9CLnmqDo24S3kB09rR7xJFxR/ZOt/+41e/OYwpx2mnsYAThzGMGTG0EXKPeg4qFwSRxCXm1tW2FYlzjLBgamk+egoDMOPyjvtr3RR2UEi+xlF5PllHrveL+TGrEi09+ZBv7pqpwyThwXE3psdRvtuq6tkYiSWUKYXBOGIaAGx6973tGEDzWFce63X3eFrFcTXYGLnKVu1yO+CzoeTt3BegQ05bF846gZnPg6geQvmGU9MmZFYu7szShlqKWKaznNfUvaDbQKzxW54MoWFgoW7WabZRpI8RRj0dGECsxzshIqUPxhQ6gwpigWNuIWoQKBgQD2DCj1Cn8en41qK+RxVLNDGDKgqeTBvMlRuHXkOokd6Pu1H1mpozbCUh9iWmt2emcXWH4yX6Ma8h5Mamn0wDOa+sOQbYqIlcR+Odo/fiO8qojaBuShgFpYKufIoTaOamtDuTD3C5ZjHbnQO7GcIB2q93+qS7aR5oVFyJIXg7BubwKBgQDL3PbZRhFPfbk5tucd6iLOLzvQo6iw9PXSfYuv7w+kHgfTWKNsaF+ybwnMpSvFE29SA3qlvLx/sjAtYarGGP4GykcPdtPuLh3ruxbgsno5e6pXJzvFn1FyOvYPhPh34YkCvl1nn45V1j2jvV/sG1gIAnjO+lVY6HO72lzfoIdawwKBgQDeNhU3WQd9bLOW05U5FTngupXerBFBN3LKL0GpoYP8U1IgPfQIVLM7xFc7yFWNZvZ8CxvN+rmfuFLMyL5woLT6nsbIRWX7/Oa86RrGhPFZjtqRrfAGO5lcT0ON2qse69V8EGC8fOaRFWOfQIojao3gvi26ay6zjfGHGKF4QyKwWQKBgHGTiXNDNiGzSZ596AUijIGSjyCODU2JK+s5Rrmd1nj4wiNdHq8m9ynR8wOwgdIqIbrvdUUhAWOjQnQGFb6H7f6aczyKV0g7KTBL61MEHFQqMZycnxBa0mVxs+SxkDw5yrtck21H12S7F1xCBU7lbQYJWoT1stb1qoTeAyATr1VBAoGAUUv0qRba1ADWTgbI5I82UxboPBF1VjyLm6n1MKti3hsZFcjYMZ11Eb+L7kQoZCIiAZ/K82AZp0H9+eUeSTbal/ZYkKGckcwu3j50Yc0F+YzDg7LrJh5REr98GQB7nw9ytJcnS3S9+rji3h8iUqcsZwtxaw6gl149G+wQX5jhpl4=";

    private static final PrivateKey PRIVATE_KEY = loadPrivateKey();

    private JwtTestSupport() { }

    private static PrivateKey loadPrivateKey() {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(TEST_PRIVATE_KEY_PKCS8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return epoch seconds (UTC) de maintenant + décalage */
    static long epochSecondsIn(Duration offset) {
        return Clock.systemUTC().instant().plus(offset).getEpochSecond();
    }

    /** @return l'en-tête Authorization "Bearer ..." avec jeton signé RS256 par la clé de test */
    static String signedBearer(JWTAuthToken token) {
        try {
            token.getHeader().setAlg("RS256");
            token.getHeader().setTyp("JWT");
            token.getPayload().setEmail_verified(true);
            String unsigned = JWTUtils.encodeJWT(token);
            Signature signature = Signature.getInstance(JWTUtils.SHA_256_WITH_RSA);
            signature.initSign(PRIVATE_KEY);
            signature.update(unsigned.getBytes());
            return "Bearer " + unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
