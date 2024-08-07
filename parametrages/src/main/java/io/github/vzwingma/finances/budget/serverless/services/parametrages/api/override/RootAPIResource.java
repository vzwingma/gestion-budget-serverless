package io.github.vzwingma.finances.budget.serverless.services.parametrages.api.override;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIResource;
import io.github.vzwingma.finances.budget.services.communs.data.model.Info;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource de base pour les API REST de l'application
 */
@Path(ParametragesAPIEnum.PARAMS_BASE)
public class RootAPIResource extends AbstractAPIResource {

    private final ParametragesService service;

    @Inject
    public RootAPIResource(ParametragesService service) {
        this.service = service;
    }

    @GET
    @Path("_info")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Uni<Info> info() {
        return Uni.combine()
                .all()
                .unis(super.info(), service.refreshJwksSigningKeys())
                .asTuple()
                .map(Tuple2::getItem1);
    }
}
