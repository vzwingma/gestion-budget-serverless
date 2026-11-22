package io.github.vzwingma.finances.budget.services.communs.api;

import jakarta.ws.rs.ext.Provider;

/**
 * Handler des exceptions métier pour les API REST, mutualisé pour les 4 services (AP004 T2.1, ADR-009).
 * Découvert par RESTEasy Reactive dans le jar {@code communs} (index Jandex).
 */
@Provider
public class APIExceptionsHandler extends AbstractAPIExceptionsHandler { }
