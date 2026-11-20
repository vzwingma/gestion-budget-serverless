package io.github.vzwingma.finances.budget.services.communs.config;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Journalise au démarrage (INFO) les valeurs <b>effectives</b> de configuration non sensibles (AP004 T2.2, ADR-009) :
 * profil actif, timeouts et pool MongoDB, timeouts OIDC, timeouts des clients REST, masquage du jeton par
 * {@code OidcProvider}. Permet de vérifier en QUA (CloudWatch) que la configuration commune de {@code communs}
 * est bien prise en compte (risques R3 et R6).
 * <p>
 * Liste blanche : seules les clés de {@link #CLES} et les timeouts des clients REST sont lus. Jamais de chaîne de
 * connexion, d'URL, de jeton ni de secret ; toute valeur qui ressemble à une URL avec identifiants est masquée.
 */
@ApplicationScoped
public class EffectiveConfigLogger {

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveConfigLogger.class);

    /** Clés non sensibles journalisées (valeur brute, sans expansion des expressions) */
    static final List<String> CLES = List.of(
            "quarkus.mongodb.database",
            "quarkus.mongodb.server-selection-timeout",
            "quarkus.mongodb.connect-timeout",
            "quarkus.mongodb.read-timeout",
            "quarkus.mongodb.max-pool-size",
            "quarkus.mongodb.min-pool-size",
            "quarkus.mongodb.max-connection-idle-time",
            "quarkus.oidc.connection-timeout",
            "quarkus.oidc.connection-delay",
            "quarkus.oidc.token.issuer",
            "quarkus.http.enable-compression",
            "quarkus.rest-client.enable-compression",
            "quarkus.log.level",
            "quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level");

    /** Timeouts et délais des clients REST (ex. {@code quarkus.rest-client.comptes-service.read-timeout}) */
    private static final Pattern CLE_TIMEOUT_CLIENT_REST = Pattern.compile("^quarkus\\.rest-client\\..+-(timeout|delay)$");

    /** URL avec identifiants ({@code scheme://user:mdp@hote}) */
    private static final Pattern URL_AVEC_IDENTIFIANTS = Pattern.compile(".*://[^/\\s]*@.*");

    static final String VALEUR_MASQUEE = "<masquée>";
    static final String VALEUR_NON_RESOLUE = "<non résolue>";

    void onStart(@Observes StartupEvent event) {
        LOG.info(decrire(ConfigProvider.getConfig(), ConfigUtils.getProfiles()));
    }

    /**
     * Construit la ligne de configuration effective
     *
     * @param config   configuration
     * @param profils  profils actifs
     * @return la ligne journalisée
     */
    static String decrire(Config config, List<String> profils) {
        List<String> elements = new ArrayList<>();
        elements.add("profils=" + profils);
        TreeSet<String> cles = new TreeSet<>(CLES);
        for (String nom : config.getPropertyNames()) {
            if (CLE_TIMEOUT_CLIENT_REST.matcher(nom).matches()) {
                cles.add(nom);
            }
        }
        for (String cle : cles) {
            elements.add(cle + "=" + valeur(config, cle));
        }
        return "[CONFIG] valeurs effectives : " + String.join(", ", elements);
    }

    private static String valeur(Config config, String cle) {
        try {
            Optional<String> valeur = config.getOptionalValue(cle, String.class);
            if (valeur.isEmpty()) {
                return "<absente>";
            }
            return URL_AVEC_IDENTIFIANTS.matcher(valeur.get()).matches() ? VALEUR_MASQUEE : valeur.get();
        } catch (RuntimeException e) {
            return VALEUR_NON_RESOLUE;
        }
    }
}
