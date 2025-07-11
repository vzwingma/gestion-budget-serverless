package io.github.vzwingma.finances.budget.serverless.services.comptes.business;


import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * Service fournissant les comptes
 *
 * @author vzwingma
 */
@ApplicationScoped
@NoArgsConstructor
public class ComptesService implements IComptesAppProvider, IJwtSigningKeyService {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComptesService.class);
    /**
     * Service Provider Interface des données
     */
    @Inject
    IComptesRepository dataComptes;

    @Inject
    IJwtSigningKeyReadRepository signingKeyReadRepository;

    public ComptesService(IComptesRepository dataComptes, IJwtSigningKeyReadRepository signingKeyReadRepository) {
        this.dataComptes = dataComptes;
        this.signingKeyReadRepository = signingKeyReadRepository;
    }

    @Override
    public Uni<Boolean> isCompteActif(String idCompte) {
        return dataComptes.isCompteActif(idCompte);
    }

    @Override
    public Uni<CompteBancaire> getCompteById(String idCompte, String idUtilisateur) {
        return dataComptes.chargeCompteParId(idCompte, idUtilisateur);
    }

    @Override
    public Uni<List<CompteBancaire>> getComptesUtilisateur(String idUtilisateur) {
        return dataComptes.chargeComptes(idUtilisateur)
                .invoke(compte -> LOGGER.trace("Compte [{}] chargé", compte.getLibelle()))
                .collect().asList()
                .onItem()
                .transform(comptes ->
                        comptes.stream()
                                .sorted(Comparator.comparingInt(CompteBancaire::getOrdre))
                                .toList());
    }

    /**
     */
    @Override
    public IJwtSigningKeyReadRepository getSigningKeyReadRepository() {
        return signingKeyReadRepository;
    }
}
