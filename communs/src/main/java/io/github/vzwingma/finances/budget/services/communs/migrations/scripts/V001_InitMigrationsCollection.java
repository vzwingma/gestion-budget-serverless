package io.github.vzwingma.finances.budget.services.communs.migrations.scripts;

import io.github.vzwingma.finances.budget.services.communs.migrations.IMongoMigration;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Migration exemple / no-op : valide de bout en bout le mécanisme de migration (découverte CDI, tri par
 * version, exécution, enregistrement dans {@code _migrations}) sans effet fonctionnel sur les données.
 * <p>
 * Sert de gabarit pour les migrations suivantes : copier cette classe dans le même package, renommer en
 * {@code V<numéro>_<description>}, adapter {@link #version()}/{@link #description()}/{@link #migrate()}.
 *
 * @author vzwingma
 */
@ApplicationScoped
public class V001_InitMigrationsCollection implements IMongoMigration {

    @Override
    public String version() {
        return "V001";
    }

    @Override
    public String description() {
        return "Initialisation du mécanisme de migrations MongoDB (no-op)";
    }

    @Override
    public Uni<Void> migrate() {
        // Aucune action fonctionnelle : valide uniquement le mécanisme de bout en bout.
        return Uni.createFrom().voidItem();
    }
}
