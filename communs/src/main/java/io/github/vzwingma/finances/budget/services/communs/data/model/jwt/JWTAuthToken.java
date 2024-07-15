package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * Modèle représentant un token JWT (JSON Web Token) avec en-tête et charge utile.
 * Permet de gérer les informations d'authentification et de session.
 */
@RegisterForReflection
@JsonDeserialize
@JsonSerialize
@Getter
@Setter
public class JWTAuthToken {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthToken.class);
    private JwtAuthHeader header; // L'en-tête du token JWT
    private JWTAuthPayload payload; // La charge utile du token JWT
    private String signature; // La signature du token JWT

    /**
     * Constructeur pour créer un token JWT avec un en-tête et une charge utile spécifiques.
     * @param header L'en-tête du token JWT.
     * @param payload La charge utile du token JWT.
     */
    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload, String signature) {
        this.header = header;
        this.payload = payload;
        this.signature = signature;
    }

    /**
     * Calcule la date et l'heure de délivrance du token à partir de la charge utile.
     * @return La date et l'heure de délivrance du token, ou null si non applicable.
     */
    @JsonIgnore
    public LocalDateTime issuedAt() {
        if (this.payload != null && this.payload.getIat() != 0) {
            return LocalDateTime.ofEpochSecond(this.getPayload().getIat(), 0, ZoneId.of("Europe/Berlin").getRules().getOffset(LocalDateTime.now()));
        }
        return null;
    }

    /**
     * Calcule la date et l'heure d'expiration du token à partir de la charge utile.
     * @return La date et l'heure d'expiration du token, ou null si non applicable.
     */
    @JsonIgnore
    public LocalDateTime expiredAt() {
        if (this.payload != null && this.payload.getExp() != 0) {
            return LocalDateTime.ofEpochSecond(this.getPayload().getExp(), 0, ZoneId.of("Europe/Berlin").getRules().getOffset(LocalDateTime.now()));
        }
        return null;
    }


    /**
     * Vérifie la validité du token JWT en fonction des paramètres de validation fournis.
     * Cette méthode effectue plusieurs vérifications pour s'assurer que le token est toujours valide :
     * - Le token ne doit pas être expiré.
     * - Le token doit provenir de Google.
     * - Le token doit être destiné à l'application utilisateur spécifiée dans les paramètres de validation.
     *
     * @param validationParams Les paramètres de validation du token, incluant l'identifiant de l'application utilisateur.
     * @return true si le token est valide selon les critères ci-dessus, false sinon.
     */
    public boolean isValid(JwtValidationParams validationParams){
        return isFromGoogle() && isFromUserAppContent(validationParams) && !isExpired() ;
    }

    /**
     * Vérifie si le token est expiré en comparant la date et l'heure actuelles à la date d'expiration.
     * @return Vrai si le token est expiré, faux sinon.
     */
    private boolean isExpired() {
        boolean isExpired = true;
        LocalDateTime expAt = expiredAt();
        if (expAt != null) {
            isExpired = !LocalDateTime.now().isBefore(expAt);
        }
        if(isExpired){
            LOG.warn("Le token est expiré depuis {}", expAt);
        }
        return isExpired;
    }

    /**
     * Vérifie si le token provient de Google.
     * @return Vrai si le token provient de Google, faux sinon.
     */
    private boolean isFromGoogle() {
        boolean isIssGood =  getPayload() != null && getPayload().getIss() != null && getPayload().getIss().contains("accounts.google.com");
        if(!isIssGood){
            LOG.warn("Le token n'est pas émis par le bon issuer (iss) : {}", getPayload() != null ? this.getPayload().getIss() : null);
        }
        return isIssGood;
    }

    /**
     * Vérifie si le token provient de l'application utilisateur.
     * @param validationParams Paramètres de validation du token.
     * @return Vrai si le token provient de l'application utilisateur, faux sinon.
     */
    private boolean isFromUserAppContent(JwtValidationParams validationParams){
        if(validationParams == null || validationParams.getIdAppUserContent() == null){
            LOG.warn("L'identifiant de l'application est nul - (Paramètre oidc.jwt.id.appusercontent)");
            return false;
        }
        String userContent = validationParams.getIdAppUserContent() + ".apps.googleusercontent.com";
        boolean isGoodAud = this.payload != null && this.payload.getAud() != null && this.payload.getAud().equals(userContent);
        if(!isGoodAud){
            LOG.warn("Le token n'est pas généré depuis l'application utilisateur [{}] : {}", validationParams.getIdAppUserContent(), this.payload != null ? this.payload.getAud() : null);
        }
        return isGoodAud;
    }


    public boolean isSigned() {
        if(this.signature != null){
            String algorithm = "SHA256withRSA";

            try{
            String publicKeyPEM = "MIIDJjCCAg6gAwIBAgIIL6zmp0dM8fQwDQYJKoZIhvcNAQEFBQAwNjE0MDIGA1UE\nAwwrZmVkZXJhdGVkLXNpZ25vbi5zeXN0ZW0uZ3NlcnZpY2VhY2NvdW50LmNvbTAe\nFw0yNDA3MDkwNDM4MzVaFw0yNDA3MjUxNjUzMzVaMDYxNDAyBgNVBAMMK2ZlZGVy\nYXRlZC1zaWdub24uc3lzdGVtLmdzZXJ2aWNlYWNjb3VudC5jb20wggEiMA0GCSqG\nSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCu/3mObL3WGJsPtxv8EIDbmO/ketftaioe\n/5+HzCs5Hlphwzwq+xzeZvOzbDW8JSUxG/s10QZUESc5nq0e9TMYuifjK2F3lexf\n7BO8C0gc+TId2jnMalOABM70ISfvxj5/UTwHLre22/XiRd79EyO3loq20rjKqpTL\nNZR7FKzukRPbNaKKB3T9saEkZhgQmUST8mw4EW+WggFrJwx8OMr76SCqeVEDxJZS\nQ/ekQkgCj2gfOa9fq5dlMQkrXRrVg2PQlVHjBljeTrQ1pRf4oKuit5TzmJiCd5zL\nFJ0v62ukp2SDT7Y5iSz7/YizKa7uM9f64pXKL7MrJqiTc3riYRFFAgMBAAGjODA2\nMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMBYGA1UdJQEB/wQMMAoGCCsG\nAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4IBAQCIEDxtAPNPoJvku5qDVq0JhPKc0nOA\nzY6eMVW8LepI+yqpcousuOTvulx63W/jqJ7mVcJ5BkCZwyD6Avs+mMETr93d1iMP\nmYrId+XsWP2fUS961foPQRzsoYLx2QzI96c6ZtEu7iimcZaI0PpnWsd745sZ3AaL\ncjyDL0wJx5AUACY4WnzupH34mzwF4dPnq8NlBE44wHIMPW0TUyO9MvAB+1831tqE\n3ilNyKmXXurFjgXMOL69GakstC41183+p/N08w+3mKT0J707q/ivQ3i5OaZFmnXs\nKzninRubv6HPlNoJcPoj0bXd9/0loztkQ8tKfy8JbcQnzgLxFxbE16Cb";
            RSAPublicKey publicKey = getPublicKeyFromString(publicKeyPEM);
            Signature publicSignature = Signature.getInstance(algorithm);
            publicSignature.initVerify(publicKey);
            publicSignature.update(signature.getBytes());
            boolean verified = publicSignature.verify(
                    Base64.getUrlDecoder().decode(signature)
            );
            System.out.printf("Signature Verified (t/f) : %b%n", verified);
            return verified;
            }
            catch (Exception e){
                LOG.error("Erreur lors de la vérification de la signature du token", e);
            }
        }
        else{
            LOG.warn("Le token n'est pas signé");
        }
        return false;
    }

    public static RSAPublicKey getPublicKeyFromString(String key) throws
            IOException, GeneralSecurityException {

        String publicKeyPEM = key;

        /**replace headers and footers of cert, if RSA PUBLIC KEY in your case, change accordingly*/
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));

        return pubKey;
    }

    @Override
    public String toString() {
        return "JWTIdToken{" +
                ", payload=" + payload +
                ", isExpired=" + isExpired() +
                '}';
    }
}
