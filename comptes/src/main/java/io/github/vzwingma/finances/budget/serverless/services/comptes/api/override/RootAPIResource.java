package io.github.vzwingma.finances.budget.serverless.services.comptes.api.override;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIResource;
import io.github.vzwingma.finances.budget.services.communs.data.model.Info;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource de base pour les API REST de l'application
 */
@Path(ComptesAPIEnum.COMPTES_BASE)
public class RootAPIResource extends AbstractAPIResource {
    @GET
    @Path("_info")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Uni<Info> info() {
        return super.info();
    }
}
