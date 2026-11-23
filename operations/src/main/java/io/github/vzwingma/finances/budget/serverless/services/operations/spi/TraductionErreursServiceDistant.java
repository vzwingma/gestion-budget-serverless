package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Liaison d'intercepteur posée sur les clients REST inter-services : traduit les échecs sans réponse HTTP
 * (connexion impossible, timeout) en {@code ServiceDistantIndisponibleException} et les trace dans {@code [REST-METRIC]}.
 *
 * <p>
 * Posée sur <b>chaque méthode</b> du client avec son nom logique et sa méthode HTTP : l'intercepteur les lit dans la liaison
 * (données générées au build par ArC), sans aucune réflexion sur l'interface REST (image native : une réflexion non
 * enregistrée lève une {@code MissingReflectionRegistrationError}). Les deux membres sont {@link Nonbinding} : ils ne
 * servent pas à choisir l'intercepteur.
 *
 * @see TraductionErreursServiceDistantInterceptor
 */
@InterceptorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TraductionErreursServiceDistant {

    /** Nom logique du client tracé dans {@code [REST-METRIC]} (ex. {@code comptes}) */
    @Nonbinding
    String client() default "inconnu";

    /** Méthode HTTP de l'appel tracée dans {@code [REST-METRIC]} */
    @Nonbinding
    String method() default "GET";
}
