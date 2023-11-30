package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config;

import io.github.vzwingma.finances.budget.services.communs.data.model.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.JwtAuthHeader;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration de la Reflection des classes {@link JWTAuthToken}, {@link JwtAuthHeader} et {@link JWTAuthPayload} pour le décodage JSON
 */
@RegisterForReflection(targets = {JWTAuthToken.class, JWTAuthPayload.class, JwtAuthHeader.class})
public class JwtReflectionConfig {
}
