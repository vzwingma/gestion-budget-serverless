package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

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
 * @see TraductionErreursServiceDistantInterceptor
 */
@InterceptorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TraductionErreursServiceDistant {
}
