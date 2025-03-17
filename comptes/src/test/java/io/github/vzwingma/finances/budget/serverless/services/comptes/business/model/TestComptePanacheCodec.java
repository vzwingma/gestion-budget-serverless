package io.github.vzwingma.finances.budget.serverless.services.comptes.business.model;

import io.github.vzwingma.finances.budget.services.communs.api.codecs.ComptePanacheCodec;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import org.bson.BsonReader;
import org.bson.json.JsonReader;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Classe de test pour la classe {@link ComptePanacheCodec}..
 * 
 * Cette classe contient plusieurs tests unitaires pour vérifier le bon fonctionnement
 * des méthodes de décodage et d'encodage de la classe ComptePanacheCodec.
 * 
 * Tests inclus :
 * - testDecodeMonoProprietaire : Vérifie le décodage d'un compte bancaire avec un seul propriétaire.
 * - testDecodeMultiProprietaire : Vérifie le décodage d'un compte bancaire avec plusieurs propriétaires.
 * - testType : Vérifie que la classe encodée est bien CompteBancaire.
 * - testDocumentId : Vérifie que l'identifiant du document est correctement généré et récupéré.
 * 
 * Scénarios de test :
 * - Les JSON d'entrée contiennent des comptes bancaires avec différentes configurations de propriétaires.
 * - Les comptes décodés doivent avoir les mêmes propriétés que celles définies dans les JSON.
 * - Les propriétés vérifiées incluent : id, libelle, itemIcon, ordre, actif, et les propriétaires.
 * 
 * Vérifications :
 * - Les objets CompteBancaire ne doivent pas être null.
 * - Les propriétés id, libelle, itemIcon, ordre, et actif doivent correspondre aux valeurs des JSON.
 * - Le nombre de propriétaires doit correspondre à celui défini dans les JSON.
 * - Les propriétés des propriétaires (id, login, libelle) doivent correspondre aux valeurs des JSON.
 */
class TestComptePanacheCodec {


    @Test
    void testDecodeMonoProprietaire() {

        final String compteUtilisateurJSON = "{\"_id\":\"test\",\"libelle\":\"BANQUE - TEST\",\"proprietaire\":{\"_id\":{\"$oid\":\"54aa7db30bc460e1aeb95596\"},\"login\":\"test\",\"libelle\":\"test\"},\"itemIcon\":\"img/banque.png\",\"ordre\":{\"$numberInt\":\"12\"},\"actif\":true,\"_class\":\"com.terrier.finances.gestion.communs.comptes.model.v12.CompteBancaire\"}";

        BsonReader reader = new JsonReader(compteUtilisateurJSON);
        CompteBancaire compteBancaire = new ComptePanacheCodec().decode(reader, null);
        assertNotNull(compteBancaire);
        assertEquals("test", compteBancaire.getId());
        assertEquals("BANQUE - TEST", compteBancaire.getLibelle());
        assertEquals("img/banque.png", compteBancaire.getItemIcon());
        assertEquals(12, compteBancaire.getOrdre());
        assertEquals(true, compteBancaire.isActif());
        assertEquals(new ObjectId("54aa7db30bc460e1aeb95596"), compteBancaire.getProprietaires().get(0).getId());
        assertEquals("test", compteBancaire.getProprietaires().get(0).getLogin());
        assertEquals("test", compteBancaire.getProprietaires().get(0).getLibelle());
    }


    /**
     * Teste la méthode de décodage pour un compte avec plusieurs propriétaires.
     * 
     * Ce test vérifie que le JSON représentant un compte bancaire avec plusieurs propriétaires
     * est correctement décodé en un objet CompteBancaire. Il vérifie également que les propriétés
     * de l'objet CompteBancaire résultant sont correctement définies.
     * 
     * Scénario de test :
     * - Le JSON d'entrée contient un compte avec deux propriétaires.
     * - Le compte décodé doit avoir les mêmes propriétés que celles définies dans le JSON.
     * - Les propriétés vérifiées incluent : id, libelle, itemIcon, ordre, actif, et les propriétaires.
     * 
     * Vérifications :
     * - L'objet CompteBancaire ne doit pas être null.
     * - Les propriétés id, libelle, itemIcon, ordre, et actif doivent correspondre aux valeurs du JSON.
     * - Le compte doit avoir exactement deux propriétaires.
     * - Les propriétés des propriétaires (id, login, libelle) doivent correspondre aux valeurs du JSON.
     */
    @Test
    void testDecodeMultiProprietaire() {

        final String compteUtilisateursJSON = "{\"_id\":\"test\",\"libelle\":\"BANQUE - TEST\",\"proprietaires\":[{\"_id\":{\"$oid\":\"54aa7db30bc460e1aeb95596\"},\"login\":\"testP1\",\"libelle\":\"testP1\"},{\"_id\":{\"$oid\":\"54aa7db30ff460e1aeb95596\"},\"login\":\"testP2\",\"libelle\":\"testP2\"}],\"itemIcon\":\"img/banque.png\",\"ordre\":{\"$numberInt\":\"12\"},\"actif\":true,\"_class\":\"com.terrier.finances.gestion.communs.comptes.model.v12.CompteBancaire\"}";

        BsonReader reader = new JsonReader(compteUtilisateursJSON);
        CompteBancaire compteBancaire = new ComptePanacheCodec().decode(reader, null);
        assertNotNull(compteBancaire);
        assertEquals("test", compteBancaire.getId());
        assertEquals("BANQUE - TEST", compteBancaire.getLibelle());
        assertEquals("img/banque.png", compteBancaire.getItemIcon());
        assertEquals(12, compteBancaire.getOrdre());
        assertEquals(true, compteBancaire.isActif());

        assertEquals(2, compteBancaire.getProprietaires().size());
        assertEquals(new ObjectId("54aa7db30bc460e1aeb95596"), compteBancaire.getProprietaires().get(0).getId());
        assertEquals("testP1", compteBancaire.getProprietaires().get(0).getLogin());
        assertEquals("testP1", compteBancaire.getProprietaires().get(0).getLibelle());
        assertEquals(new ObjectId("54aa7db30ff460e1aeb95596"), compteBancaire.getProprietaires().get(1).getId());
        assertEquals("testP2", compteBancaire.getProprietaires().get(1).getLogin());
        assertEquals("testP2", compteBancaire.getProprietaires().get(1).getLibelle());        
    }



    @Test
    void testType() {
        assertEquals(CompteBancaire.class, new ComptePanacheCodec().getEncoderClass());
    }


    /*
    @Test : test pour vérifier qu'un documentId est bien généré
     */
    @Test
    void testDocumentId() {
        CompteBancaire compte1 = new CompteBancaire();
        assertNotNull(new ComptePanacheCodec().getDocumentId(compte1).toString());

        CompteBancaire compte2 = new CompteBancaire();
        compte2.setId("54aa7db30bc460e1aeb95596");
        assertEquals("54aa7db30bc460e1aeb95596", (new ComptePanacheCodec().getDocumentId(compte2)).asString().getValue());
    }

}
