package io.github.vzwingma.finances.budget.services.communs.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

    /**
     * Constructeur pour créer un token JWT avec un en-tête et une charge utile spécifiques.
     * @param header L'en-tête du token JWT.
     * @param payload La charge utile du token JWT.
     */
    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload) {
        this.header = header;
        this.payload = payload;
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
     * Vérifie si le token est toujours valide en comparant la date et l'heure actuelles à la date d'expiration.
     * @return Vrai si le token n'est pas expiré, faux sinon.
     */
    public boolean isValid(){
        return !isExpired();
    }

    /**
     * Vérifie si le token est expiré en comparant la date et l'heure actuelles à la date d'expiration.
     * @return Vrai si le token est expiré, faux sinon.
     */
    private boolean isExpired() {
        LocalDateTime expAt = expiredAt();
        if (expAt != null) {
            return !LocalDateTime.now().isBefore(expAt);
        }
        return false;
    }

    @Override
    public String toString() {
        return "JWTIdToken{" +
                ", payload=" + payload +
                ", isExpired=" + isExpired() +
                '}';
    }
}
