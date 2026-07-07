package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration de la Reflection des classes {@link MigrationRecord} et {@link MigrationRecord.MigrationStatutEnum}
 * pour la (dé)sérialisation BSON/MongoDB en mode natif GraalVM.
 * <p>
 * Note : {@link MigrationRecord} porte déjà {@code @RegisterForReflection} directement (comme {@code JwksAuthKey}
 * dans {@code communs}), cette classe centralise le hint pour cohérence avec le pattern {@code JwtReflectionConfig}
 * utilisé dans chaque microservice et sert de point d'extension si d'autres classes migrations nécessitent un hint.
 */
@RegisterForReflection(targets = {MigrationRecord.class, MigrationRecord.MigrationStatutEnum.class})
public class MigrationReflectionConfig { }
