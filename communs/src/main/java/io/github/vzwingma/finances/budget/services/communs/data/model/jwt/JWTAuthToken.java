package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

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

    private JwtAuthHeader header; // L'en-tête du token JWT
    private JWTAuthPayload payload; // La charge utile du token JWT
    private boolean hasSignature; // Indique si le token porte une signature
    private String rawContent; // Le contenu brut du token JWT

    /**
     * Constructeur pour créer un token JWT avec un en-tête et une charge utile spécifiques.
     * @param header L'en-tête du token JWT.
     * @param payload La charge utile du token JWT.
     */
    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload){
        this.header = header;
        this.payload = payload;
        this.hasSignature = false;
        this.rawContent = null;
    }
    /**
     * Constructeur pour créer un token JWT avec un en-tête et une charge utile spécifiques.
     * @param header L'en-tête du token JWT.
     * @param payload La charge utile du token JWT.
     * @param hasSignature Indique si le token porte une signature.
     * @param rawContent Le contenu brut du token JWT.
     */
    public JWTAuthToken(JwtAuthHeader header, JWTAuthPayload payload, boolean hasSignature, String rawContent) {
        this.header = header;
        this.payload = payload;
        this.hasSignature = hasSignature;
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


}
