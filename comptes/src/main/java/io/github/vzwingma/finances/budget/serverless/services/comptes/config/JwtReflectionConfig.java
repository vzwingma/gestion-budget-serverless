package io.github.vzwingma.finances.budget.serverless.services.comptes.config;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.*;
import io.quarkus.runtime.annotations.RegisterForReflection;


/**
 * Configuration de la Reflection des classes {@link JWTAuthToken}, {@link JwtAuthHeader}, {@link JWTAuthPayload}, {@link JwksAuthKeys} et {@link JwksAuthKey} pour le décodage JSON
 */
@RegisterForReflection(targets = {JWTAuthToken.class, JWTAuthPayload.class, JwtAuthHeader.class, JwksAuthKeys.class, JwksAuthKey.class})
public class JwtReflectionConfig { }
