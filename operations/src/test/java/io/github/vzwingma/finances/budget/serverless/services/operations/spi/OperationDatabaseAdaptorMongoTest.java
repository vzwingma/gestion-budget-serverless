package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.OperationsService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.TotauxCategorie;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationTypeEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.LibellesReference;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.LibellesReference.OperationBrute;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperationTypeEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration de {@link OperationDatabaseAdaptor} contre un vrai MongoDB (Quarkus Dev Services, Docker requis :
 * exécutés par la CI, pas en local sans Docker) : tri {@code _id}, lecture par {@code _id}, dernier budget &le; M,
 * compensation ciblée ({@code $pull}, {@code $set} avec {@code arrayFilters}, {@code $push}/{@code $position}, encodage
 * BSON des POJO) et agrégation des libellés ({@code $group}).
 * <p>
 * <b>MongoDB de test</b> : {@link MongoDbTestResource} (Testcontainers) fournit la chaîne de connexion et Dev Services est
 * désactivé par le {@link TestProfile} ci-dessous. Raison : le profil Maven {@code native} (CI « native snapshot apps »)
 * embarque {@code prod/application.properties}, qui définit {@code quarkus.mongodb.connection-string} et empêche Dev Services
 * de démarrer ; le test tapait alors {@code localhost:27017} (MongoTimeoutException). Le test est ainsi indépendant du profil
 * Maven. Nommage {@code *Test} (et non {@code *IT}) : seul surefire est configuré pour les modules (failsafe n'existe que
 * dans le profil {@code native}), un {@code *IT} ne serait jamais exécuté par la CI.
 * <p>
 * Chaque test utilise un identifiant de compte unique : aucun nettoyage, aucune dépendance entre tests.
 *
 * @author QALvin
 */
@QuarkusTest
@TestProfile(OperationDatabaseAdaptorMongoTest.ProfilBaseDediee.class)
class OperationDatabaseAdaptorMongoTest {

    /** Isole la base de ces tests, désactive Dev Services et branche le MongoDB Testcontainers */
    public static class ProfilBaseDediee implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.mongodb.database", "operations-it",
                    "quarkus.mongodb.devservices.enabled", "false");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(MongoDbTestResource.class));
        }
    }

    @Inject
    OperationDatabaseAdaptor adaptor;

    private static String nouveauCompte() {
        return "it" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static CompteBancaire compte(String id) {
        CompteBancaire compte = new CompteBancaire();
        compte.setId(id);
        return compte;
    }

    private static LigneOperation ligne(String id, String libelle, String cat, String ssCat, double valeur) {
        LigneOperation operation = new LigneOperation();
        operation.setId(id);
        operation.setLibelle(libelle);
        if (cat != null) {
            LigneOperation.Categorie categorie = new LigneOperation.Categorie();
            categorie.setId(cat);
            categorie.setLibelle("Catégorie " + cat);
            operation.setCategorie(categorie);
        }
        if (ssCat != null) {
            LigneOperation.SsCategorie ssCategorie = new LigneOperation.SsCategorie();
            ssCategorie.setId(ssCat);
            ssCategorie.setLibelle("Sous-catégorie " + ssCat);
            ssCategorie.setType(CategorieOperationTypeEnum.ESSENTIEL);
            operation.setSsCategorie(ssCategorie);
        }
        operation.setTypeOperation(OperationTypeEnum.DEPENSE);
        operation.setEtat(OperationEtatEnum.REALISEE);
        operation.setValeur(valeur);
        LigneOperation.Mensualite mensualite = new LigneOperation.Mensualite();
        mensualite.setPeriode(OperationPeriodiciteEnum.TRIMESTRIELLE);
        mensualite.setProchaineEcheance(2);
        mensualite.setDateFin(LocalDate.of(2027, 1, 31));
        operation.setMensualite(mensualite);
        LigneOperation.AddInfos infos = new LigneOperation.AddInfos();
        infos.setDateCreate(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        infos.setDateOperation(LocalDate.of(2026, 1, 15));
        infos.setDateMaj(LocalDateTime.of(2026, 2, 3, 4, 5, 6));
        infos.setAuteur("it");
        operation.setAutresInfos(infos);
        return operation;
    }

    private static BudgetMensuel budget(String idCompte, Month mois, int annee, LigneOperation... operations) {
        BudgetMensuel budget = new BudgetMensuel();
        budget.setIdCompteBancaire(idCompte);
        budget.setMois(mois);
        budget.setAnnee(annee);
        budget.setId(BudgetMensuel.getBudgetId(idCompte, mois, annee));
        budget.setActif(true);
        budget.setDateMiseAJour(LocalDateTime.of(2026, 1, 1, 0, 0));
        budget.setListeOperations(new ArrayList<>(List.of(operations)));
        budget.getSoldes().setSoldeAtFinMoisPrecedent(10D);
        budget.getSoldes().setSoldeAtMaintenant(20D);
        budget.getSoldes().setSoldeAtFinMoisCourant(30D);
        return budget;
    }

    private BudgetMensuel sauvegarde(BudgetMensuel budget) {
        return adaptor.sauvegardeBudgetMensuel(budget).await().indefinitely();
    }

    private BudgetMensuel relit(BudgetMensuel budget) {
        return adaptor.chargeBudgetMensuel(budget.getId()).await().indefinitely();
    }

    private static Throwable erreur(Uni<?> uni) {
        Throwable erreur = assertThrows(Throwable.class, () -> uni.await().indefinitely());
        return erreur instanceof CompletionException && erreur.getCause() != null ? erreur.getCause() : erreur;
    }

    private static void assertLigneIdentique(LigneOperation attendue, LigneOperation lue) {
        assertEquals(attendue.getId(), lue.getId());
        assertEquals(attendue.getLibelle(), lue.getLibelle());
        assertEquals(attendue.getValeur(), lue.getValeur());
        assertEquals(attendue.getTypeOperation(), lue.getTypeOperation());
        assertEquals(attendue.getEtat(), lue.getEtat());
        assertEquals(attendue.getCategorie().getId(), lue.getCategorie().getId());
        assertEquals(attendue.getSsCategorie().getId(), lue.getSsCategorie().getId());
        assertEquals(attendue.getSsCategorie().getType(), lue.getSsCategorie().getType());
        assertEquals(attendue.getMensualite().getPeriode(), lue.getMensualite().getPeriode());
        assertEquals(attendue.getMensualite().getProchaineEcheance(), lue.getMensualite().getProchaineEcheance());
        assertEquals(attendue.getMensualite().getDateFin(), lue.getMensualite().getDateFin());
        assertEquals(attendue.getAutresInfos().getDateCreate(), lue.getAutresInfos().getDateCreate());
        assertEquals(attendue.getAutresInfos().getDateOperation(), lue.getAutresInfos().getDateOperation());
        assertEquals(attendue.getAutresInfos().getDateMaj(), lue.getAutresInfos().getDateMaj());
        assertEquals(attendue.getAutresInfos().getAuteur(), lue.getAutresInfos().getAuteur());
    }

    private static List<String> ids(BudgetMensuel budget) {
        return budget.getListeOperations().stream().map(LigneOperation::getId).toList();
    }

    // ====== tri _id ======

    @Test
    void testChargeSoldes_triParIdChronologique_independantDeLOrdreDInsertion() {
        String idCompte = nouveauCompte();
        // Insertion volontairement dans le désordre : sans Sort("_id"), l'ordre naturel serait celui de l'insertion
        sauvegarde(budget(idCompte, Month.MARCH, 2026));
        sauvegarde(budget(idCompte, Month.JANUARY, 2026));
        sauvegarde(budget(idCompte, Month.DECEMBER, 2025));
        sauvegarde(budget(idCompte, Month.FEBRUARY, 2026));

        List<ProjectionBudgetSoldes> soldes = adaptor.chargeSoldesBudgetMensuel(idCompte, null, null).collect().asList().await().indefinitely();

        assertEquals(List.of("2025-DECEMBER", "2026-JANUARY", "2026-FEBRUARY", "2026-MARCH"),
                soldes.stream().map(s -> s.getAnnee() + "-" + s.getMois()).toList(),
                "Les budgets doivent être triés par _id (chronologique)");
    }

    @Test
    void testChargeSoldes_filtreAnneeEtMois_triParId() {
        String idCompte = nouveauCompte();
        sauvegarde(budget(idCompte, Month.MARCH, 2026));
        sauvegarde(budget(idCompte, Month.JANUARY, 2026));
        sauvegarde(budget(idCompte, Month.JANUARY, 2027));

        List<ProjectionBudgetSoldes> annee2026 = adaptor.chargeSoldesBudgetMensuel(idCompte, null, 2026).collect().asList().await().indefinitely();
        assertEquals(List.of(Month.JANUARY, Month.MARCH), annee2026.stream().map(ProjectionBudgetSoldes::getMois).toList());

        List<ProjectionBudgetSoldes> janvier = adaptor.chargeSoldesBudgetMensuel(idCompte, Month.JANUARY, 2027).collect().asList().await().indefinitely();
        assertEquals(1, janvier.size());
        assertEquals(2027, janvier.getFirst().getAnnee());
    }

    @Test
    void testOverrideLibelles_remplaceLesLibellesDansTousLesBudgetsDuCompte() {
        String idCompte = nouveauCompte();
        sauvegarde(budget(idCompte, Month.MARCH, 2026, ligne("m1", "carrefour city", "c1", "s1", 1)));
        sauvegarde(budget(idCompte, Month.JANUARY, 2026, ligne("j1", "carrefour market", "c1", "s1", 2), ligne("j2", "autre", "c1", "s1", 3)));

        LibelleAvantApres remplacement = new LibelleAvantApres();
        remplacement.setAvant("carrefour");
        remplacement.setApres("Carrefour");
        List<BudgetMensuel> modifies = adaptor.overrideLibellesOperations(idCompte, List.of(remplacement))
                .collect().asList().await().indefinitely();

        assertEquals(2, modifies.size());
        assertEquals("Carrefour city", relit(budget(idCompte, Month.MARCH, 2026)).getListeOperations().getFirst().getLibelle());
        assertEquals(List.of("Carrefour market", "autre"),
                relit(budget(idCompte, Month.JANUARY, 2026)).getListeOperations().stream().map(LigneOperation::getLibelle).toList());
    }

    // ====== lecture par _id ======

    @Test
    void testChargeBudgetMensuel_parCompteMoisAnnee_etParId() {
        String idCompte = nouveauCompte();
        BudgetMensuel janvier = sauvegarde(budget(idCompte, Month.JANUARY, 2026, ligne("a", "A", "c1", "s1", 1)));

        BudgetMensuel parChamps = adaptor.chargeBudgetMensuel(compte(idCompte), Month.JANUARY, 2026).await().indefinitely();
        BudgetMensuel parId = adaptor.chargeBudgetMensuel(janvier.getId()).await().indefinitely();

        assertEquals(janvier.getId(), parChamps.getId());
        assertEquals(janvier.getId(), parId.getId());
        assertEquals(Month.JANUARY, parId.getMois());
        assertLigneIdentique(ligne("a", "A", "c1", "s1", 1), parId.getListeOperations().getFirst());
        assertTrue(adaptor.isBudgetActif(janvier.getId()).await().indefinitely());
    }

    @Test
    void testChargeBudgetMensuel_absent_budgetNotFound() {
        String idCompte = nouveauCompte();

        assertInstanceOf(BudgetNotFoundException.class, erreur(adaptor.chargeBudgetMensuel(compte(idCompte), Month.JANUARY, 2026)));
        assertInstanceOf(BudgetNotFoundException.class, erreur(adaptor.chargeBudgetMensuel(BudgetMensuel.getBudgetId(idCompte, Month.JANUARY, 2026))));
    }

    // ====== dernier budget <= M ======

    @Test
    void testChargeDernierBudgetAvant_borneMIncluse_etDernierBudgetAnterieur() {
        String idCompte = nouveauCompte();
        sauvegarde(budget(idCompte, Month.JANUARY, 2026));
        sauvegarde(budget(idCompte, Month.MARCH, 2026));
        sauvegarde(budget(idCompte, Month.JUNE, 2026));

        // Borne M incluse
        assertEquals(BudgetMensuel.getBudgetId(idCompte, Month.MARCH, 2026),
                adaptor.chargeDernierBudgetAvant(idCompte, Month.MARCH, 2026).await().indefinitely().getId());
        // M absent : le dernier budget antérieur, jamais un budget postérieur
        assertEquals(BudgetMensuel.getBudgetId(idCompte, Month.MARCH, 2026),
                adaptor.chargeDernierBudgetAvant(idCompte, Month.MAY, 2026).await().indefinitely().getId());
        assertEquals(BudgetMensuel.getBudgetId(idCompte, Month.JANUARY, 2026),
                adaptor.chargeDernierBudgetAvant(idCompte, Month.FEBRUARY, 2026).await().indefinitely().getId());
        // Changement d'année
        assertEquals(BudgetMensuel.getBudgetId(idCompte, Month.JUNE, 2026),
                adaptor.chargeDernierBudgetAvant(idCompte, Month.JANUARY, 2027).await().indefinitely().getId());
    }

    @Test
    void testChargeDernierBudgetAvant_aucunBudget_null() {
        String idCompte = nouveauCompte();
        sauvegarde(budget(idCompte, Month.JUNE, 2026));

        assertNull(adaptor.chargeDernierBudgetAvant(idCompte, Month.MAY, 2026).await().indefinitely(), "Aucun budget <= M : null");
        assertNull(adaptor.chargeDernierBudgetAvant(nouveauCompte(), Month.MAY, 2026).await().indefinitely(), "Compte sans aucun budget : null");
    }

    @Test
    void testChargeDernierBudgetAvant_idCompteContenantUnUnderscore() {
        String idCompte = nouveauCompte() + "_x";
        sauvegarde(budget(idCompte, Month.JANUARY, 2026));
        sauvegarde(budget(idCompte, Month.FEBRUARY, 2026));

        assertEquals(BudgetMensuel.getBudgetId(idCompte, Month.FEBRUARY, 2026),
                adaptor.chargeDernierBudgetAvant(idCompte, Month.MARCH, 2026).await().indefinitely().getId());
    }

    @Test
    void testChargeDernierBudgetAvant_deuxComptesDeMemePrefixe_aEtAUnderscore1() {
        String a = nouveauCompte();
        String a1 = a + "_1";
        sauvegarde(budget(a, Month.JANUARY, 2026));
        sauvegarde(budget(a1, Month.MAY, 2026));
        // L'_id « a_1_2026_05 » est compris entre les bornes d'_id du compte « a » (« a_ » <= _id <= « a_2026_06 »)
        // sans lui appartenir : seule l'égalité sur idCompteBancaire l'écarte.
        BudgetMensuel pourA = adaptor.chargeDernierBudgetAvant(a, Month.JUNE, 2026).await().indefinitely();
        assertEquals(BudgetMensuel.getBudgetId(a, Month.JANUARY, 2026), pourA.getId(), "Le budget du compte « a_1 » ne doit pas être retourné pour « a »");
        assertEquals(a, pourA.getIdCompteBancaire());

        BudgetMensuel pourA1 = adaptor.chargeDernierBudgetAvant(a1, Month.JUNE, 2026).await().indefinitely();
        assertEquals(BudgetMensuel.getBudgetId(a1, Month.MAY, 2026), pourA1.getId());
        assertEquals(a1, pourA1.getIdCompteBancaire());
    }

    // ====== compensation ciblée ======

    /** Budget recalculé de compensation : soldes/totaux différents de ceux stockés, pour vérifier leur réécriture */
    private static BudgetMensuel recalcule(BudgetMensuel base, LigneOperation... operations) {
        BudgetMensuel recalcule = budget(base.getIdCompteBancaire(), base.getMois(), base.getAnnee(), operations);
        recalcule.getSoldes().setSoldeAtFinMoisPrecedent(100D);
        recalcule.getSoldes().setSoldeAtMaintenant(200D);
        recalcule.getSoldes().setSoldeAtFinMoisCourant(300D);
        TotauxCategorie total = new TotauxCategorie();
        total.setLibelleCategorie("Catégorie c1");
        total.setTotalAtMaintenant(-12.5D);
        total.setTotalAtFinMoisCourant(-42D);
        recalcule.setTotauxParCategories(new HashMap<>(Map.of("c1", total)));
        recalcule.setTotauxParSSCategories(new HashMap<>(Map.of("s1", total)));
        recalcule.setTotauxParTypeCategories(new HashMap<>(Map.of("ESSENTIEL", total)));
        recalcule.setDateMiseAJour(LocalDateTime.of(2026, 7, 10, 10, 15, 30));
        return recalcule;
    }

    private static void assertChampsCalculesReecrits(BudgetMensuel lu) {
        assertEquals(100D, lu.getSoldes().getSoldeAtFinMoisPrecedent());
        assertEquals(200D, lu.getSoldes().getSoldeAtMaintenant());
        assertEquals(300D, lu.getSoldes().getSoldeAtFinMoisCourant());
        assertEquals(-12.5D, lu.getTotauxParCategories().get("c1").getTotalAtMaintenant());
        assertEquals(-42D, lu.getTotauxParSSCategories().get("s1").getTotalAtFinMoisCourant());
        assertEquals("Catégorie c1", lu.getTotauxParTypeCategories().get("ESSENTIEL").getLibelleCategorie());
        assertEquals(LocalDateTime.of(2026, 7, 10, 10, 15, 30), lu.getDateMiseAJour());
    }

    @Test
    void testCompense_creation_pullDeLOperation_autresChampsInchanges() {
        String idCompte = nouveauCompte();
        LigneOperation a = ligne("a", "A", "c1", "s1", 1);
        LigneOperation b = ligne("b", "B", "c1", "s1", 2);
        LigneOperation nouvelle = ligne("nouvelle", "Virement", "c1", "s1", 3);
        BudgetMensuel enBase = sauvegarde(budget(idCompte, Month.JANUARY, 2026, a, b, nouvelle));

        adaptor.compenseOperation(recalcule(enBase, a, b), "nouvelle", null, -1).await().indefinitely();

        BudgetMensuel lu = relit(enBase);
        assertEquals(List.of("a", "b"), ids(lu), "Seule l'opération créée est retirée");
        assertLigneIdentique(a, lu.getListeOperations().get(0));
        assertChampsCalculesReecrits(lu);
        assertTrue(lu.isActif(), "Les autres champs du budget ne sont pas réécrits");
        assertEquals(Month.JANUARY, lu.getMois());
        assertEquals(idCompte, lu.getIdCompteBancaire());
    }

    @Test
    void testCompense_creation_budgetAbsent_budgetNotFound() {
        BudgetMensuel absent = budget(nouveauCompte(), Month.JANUARY, 2026);

        assertInstanceOf(BudgetNotFoundException.class, erreur(adaptor.compenseOperation(recalcule(absent), "x", null, -1)));
    }

    @Test
    void testCompense_modification_setPositionnelArrayFilters_restaureLaLigneEtLaLaisseLesAutres() {
        String idCompte = nouveauCompte();
        LigneOperation a = ligne("a", "A", "c1", "s1", 1);
        LigneOperation bOrigine = ligne("b", "B d'origine", "c1", "s1", 2);
        LigneOperation bModifiee = ligne("b", "B modifiée", "c2", "s3", 99);
        LigneOperation c = ligne("c", "C", "c1", "s1", 3);
        BudgetMensuel enBase = sauvegarde(budget(idCompte, Month.JANUARY, 2026, a, bModifiee, c));

        adaptor.compenseOperation(recalcule(enBase, a, bOrigine, c), "b", bOrigine, 1).await().indefinitely();

        BudgetMensuel lu = relit(enBase);
        assertEquals(List.of("a", "b", "c"), ids(lu), "Ordre de la liste inchangé");
        assertLigneIdentique(bOrigine, lu.getListeOperations().get(1));
        assertLigneIdentique(a, lu.getListeOperations().get(0));
        assertLigneIdentique(c, lu.getListeOperations().get(2));
        assertChampsCalculesReecrits(lu);
    }

    @Test
    void testCompense_modification_ligneDisparue_pushAuRangOriginal() {
        String idCompte = nouveauCompte();
        LigneOperation a = ligne("a", "A", "c1", "s1", 1);
        LigneOperation b = ligne("b", "B", "c1", "s1", 2);
        LigneOperation c = ligne("c", "C", "c1", "s1", 3);
        // La ligne « b » a disparu de la base (suppression concurrente) : elle doit être ré-insérée à son rang d'origine (1)
        BudgetMensuel enBase = sauvegarde(budget(idCompte, Month.JANUARY, 2026, a, c));

        adaptor.compenseOperation(recalcule(enBase, a, b, c), "b", b, 1).await().indefinitely();

        BudgetMensuel lu = relit(enBase);
        assertEquals(List.of("a", "b", "c"), ids(lu));
        assertLigneIdentique(b, lu.getListeOperations().get(1));
        assertChampsCalculesReecrits(lu);
    }

    @Test
    void testCompense_modification_ligneDisparue_rangInconnuOuHorsListe_pushEnFinDeListe() {
        String idCompte = nouveauCompte();
        LigneOperation a = ligne("a", "A", "c1", "s1", 1);
        LigneOperation b = ligne("b", "B", "c1", "s1", 2);
        BudgetMensuel inconnu = sauvegarde(budget(idCompte, Month.JANUARY, 2026, a));
        adaptor.compenseOperation(recalcule(inconnu, a, b), "b", b, -1).await().indefinitely();
        assertEquals(List.of("a", "b"), ids(relit(inconnu)), "Rang inconnu : en fin de liste");

        String autreCompte = nouveauCompte();
        BudgetMensuel horsListe = sauvegarde(budget(autreCompte, Month.JANUARY, 2026, ligne("a", "A", "c1", "s1", 1)));
        adaptor.compenseOperation(recalcule(horsListe, a, b), "b", b, 12).await().indefinitely();
        assertEquals(List.of("a", "b"), ids(relit(horsListe)), "$position au-delà de la taille : en fin de liste");
    }

    @Test
    void testCompense_modification_budgetAbsent_illegalStateException() {
        BudgetMensuel absent = budget(nouveauCompte(), Month.JANUARY, 2026);
        LigneOperation b = ligne("b", "B", "c1", "s1", 2);

        assertInstanceOf(IllegalStateException.class, erreur(adaptor.compenseOperation(recalcule(absent, b), "b", b, 0)));
    }

    @Test
    void testCompense_idAbsent_refuseSansToucherALaBase() {
        String idCompte = nouveauCompte();
        LigneOperation a = ligne("a", "A", "c1", "s1", 1);
        BudgetMensuel enBase = sauvegarde(budget(idCompte, Month.JANUARY, 2026, a));

        assertInstanceOf(IllegalArgumentException.class, erreur(adaptor.compenseOperation(recalcule(enBase), null, null, -1)));

        assertEquals(List.of("a"), ids(relit(enBase)));
        assertEquals(30D, relit(enBase).getSoldes().getSoldeAtFinMoisCourant(), "Aucune écriture");
    }

    // ====== agrégation des libellés ======

    private static String cle(Document id) {
        return id.getString("libelle") + "|" + id.getString("categorieId") + "|" + id.getString("ssCategorieId");
    }

    /** Répartit les opérations brutes sur trois budgets du compte (une opération sur trois par mois) */
    private void sauvegardeJeuDeDonnees(String idCompte, List<OperationBrute> brutes) {
        List<List<LigneOperation>> parMois = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < brutes.size(); i++) {
            OperationBrute brute = brutes.get(i);
            parMois.get(i % 3).add(ligne("op-" + i, brute.libelle(), brute.categorieId(), brute.ssCategorieId(), i));
        }
        Month[] mois = {Month.JANUARY, Month.FEBRUARY, Month.MARCH};
        for (int m = 0; m < 3; m++) {
            sauvegarde(budget(idCompte, mois[m], 2026, parMois.get(m).toArray(new LigneOperation[0])));
        }
    }

    @Test
    void testGetLibellesOperations_groupeParValeurBruteAvecCompteur() {
        String idCompte = nouveauCompte();
        List<OperationBrute> brutes = LibellesReference.jeuDeDonnees();
        sauvegardeJeuDeDonnees(idCompte, brutes);
        // Un autre compte : ne doit pas être agrégé
        sauvegarde(budget(nouveauCompte(), Month.JANUARY, 2026, ligne("autre", "Courses", "c1", "s1", 1)));

        List<Document> groupes = adaptor.getLibellesOperations(idCompte).collect().asList().await().indefinitely();

        Map<String, Integer> attendu = new HashMap<>();
        brutes.forEach(op -> attendu.merge(op.libelle() + "|" + op.categorieId() + "|" + op.ssCategorieId(), 1, Integer::sum));
        Map<String, Integer> obtenu = new HashMap<>();
        groupes.forEach(g -> obtenu.put(cle(g.get("_id", Document.class)), ((Number) g.get("n")).intValue()));
        assertEquals(attendu, obtenu, "Un groupe par (libellé, catégorie, sous-catégorie) avec son nombre d'occurrences");
    }

    @Test
    void testGetLibellesOperations_compteSansOperation_aucunGroupe() {
        String idCompte = nouveauCompte();
        sauvegarde(budget(idCompte, Month.JANUARY, 2026));

        assertTrue(adaptor.getLibellesOperations(idCompte).collect().asList().await().indefinitely().isEmpty());
        assertTrue(adaptor.getLibellesOperations(nouveauCompte()).collect().asList().await().indefinitely().isEmpty());
    }

    @Test
    void testParite_ancienEtNouvelAlgorithme_surMongoReel() {
        String idCompte = nouveauCompte();
        List<OperationBrute> brutes = LibellesReference.jeuDeDonnees();
        sauvegardeJeuDeDonnees(idCompte, brutes);

        IParametragesServiceProvider parametrages = mock(IParametragesServiceProvider.class);
        when(parametrages.getCategories()).thenReturn(Uni.createFrom().item(LibellesReference.categories()));
        OperationsService service = new OperationsService();
        service.setDataOperationsProvider(adaptor);
        service.setParametragesService(parametrages);

        List<LibelleCategorieOperation> nouveau = service.getLibellesOperations(idCompte).collect().asList().await().indefinitely();
        List<LibelleCategorieOperation> ancien = LibellesReference.ancienAlgorithme(brutes, LibellesReference.categories());

        assertFalse(ancien.isEmpty());
        assertEquals(LibellesReference.resume(ancien), LibellesReference.resume(nouveau),
                "Parité C3 sur MongoDB réel : $group + somme des compteurs = ancien comptage en Java");
        assertNotNull(nouveau.getFirst().getLibelle());
    }
}
