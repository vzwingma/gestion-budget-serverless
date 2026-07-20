/**
 *
 */
package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model;

import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Définition d'un utilisateur de la BDD
 *
 * @author vzwingma
 */
@MongoEntity(collection = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur implements Serializable {

    @Serial
    private static final long serialVersionUID = -1019487824881674829L;
    //ID
    private ObjectId id;
    // Login
    private String login;

    private LocalDateTime dernierAcces;

    /**
     * Préférences
     */
    private Map<UtilisateurPrefsEnum, String> prefsUtilisateur = new EnumMap<>(UtilisateurPrefsEnum.class);
    /**
     * Droits
     */
    private Map<UtilisateurDroitsEnum, Boolean> droits = new EnumMap<>(UtilisateurDroitsEnum.class);


    /**
     * clone d'utilisateur (horloge système UTC)
     */
    public Utilisateur(Utilisateur source) {
        this(source, Clock.systemUTC());
    }

    /**
     * clone d'utilisateur
     *
     * @param source utilisateur source à cloner
     * @param clock  horloge applicative (ADR-004) utilisée pour dater le dernier accès
     */
    public Utilisateur(Utilisateur source, Clock clock) {
        setId(source.getId());
        setDernierAcces(LocalDateTime.now(clock));
        setLogin(source.getLogin());
        setDroits(source.getDroits());
        setPrefsUtilisateur(source.getPrefsUtilisateur());
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.login;
    }

    public String toFullString() {
        return String.format("Utilisateur [id=%s, dateDernerAcces=%s]", this.login, dernierAcces != null ? BudgetDateTimeUtils.getLibelleDate(dernierAcces) : "nulle");
    }
}
