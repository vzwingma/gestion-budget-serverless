package io.github.vzwingma.finances.budget.services.communs.spi;

import jakarta.ws.rs.ext.Provider;

/**
 * Handler des exceptions d'accès base de données (toute {@link com.mongodb.MongoException} : 503), mutualisé pour les
 * 4 services (AP004 T2.1, ADR-009).
 */
@Provider
public class SPIExceptionsHandler extends AbstractBDDExceptionsHandler { }
