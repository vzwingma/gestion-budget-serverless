package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
    private String rawContent; // Le contenu brut du token JWT

    /**
     * Constructeur pour créer un token JWT avec un en-tête et une charge utile spécifiques.
     * @param header L'en-tête du token JWT.
     * @param payload La charge utile du token JWT.
     */
    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload, String rawContent) {
        this.header = header;
        this.payload = payload;
        this.rawContent = rawContent;
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
        return isFromGoogle() && isFromUserAppContent(validationParams) && isSigned(validationParams) && !isExpired() ;
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


    /**
     * Vérifie si le token JWT est signé en utilisant les clés publiques de Google.
     * @return true si la signature est valide, false sinon.
     */
    public boolean isSigned(JwtValidationParams validationParams) {
        if(this.rawContent != null){
            return JWTUtils.isTokenSigValid(this.rawContent, validationParams.getJwksAuthKeys());  // Vérifie la signature du token JWT
        }
        else{
            LOG.warn("Le token n'est pas signé");
        }
        return false;
    }
}
