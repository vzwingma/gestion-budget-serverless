package io.github.vzwingma.finances.budget.serverless.services.comptes.business.model;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.api.codecs.ComptePanacheCodec;
import org.bson.BsonReader;
import org.bson.json.JsonReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link ComptePanacheCodec}.
 */
class TestComptePanacheCodec {


    @Test
    void testDecode() {

        final String utilisateurJSON = "{\"_id\":\"test\",\"libelle\":\"BANQUE - TEST\",\"proprietaire\":{\"_id\":{\"$oid\":\"54aa7db30bc460e1aeb95596\"},\"login\":\"test\",\"libelle\":\"test\"},\"itemIcon\":\"img/banque.png\",\"ordre\":{\"$numberInt\":\"12\"},\"actif\":true,\"_class\":\"com.terrier.finances.gestion.communs.comptes.model.v12.CompteBancaire\"}";

        BsonReader reader = new JsonReader(utilisateurJSON);
        CompteBancaire compteBancaire = new ComptePanacheCodec().decode(reader, null);
        assertNotNull(compteBancaire);
        assertEquals("test", compteBancaire.getId());
        assertEquals("BANQUE - TEST", compteBancaire.getLibelle());
        assertEquals("img/banque.png", compteBancaire.getItemIcon());
        assertEquals(12, compteBancaire.getOrdre());
        assertEquals(true, compteBancaire.isActif());
      //  assertEquals(new ObjectId("54aa7db30bc460e1aeb95596"), compteBancaire.getProprietaire().getId());
        assertEquals("test", compteBancaire.getProprietaire().getLogin());
        assertEquals("test", compteBancaire.getProprietaire().getLibelle());
    }

    @Test
    void testType(){
        assertEquals(CompteBancaire.class, new ComptePanacheCodec().getEncoderClass());
    }


    /*
    @Test : test pour vérifier qu'un documentId est bien généré
     */
    @Test
    void testDocumentId(){
        CompteBancaire compte1 = new CompteBancaire();
        assertNotNull(new ComptePanacheCodec().getDocumentId(compte1).toString());

        CompteBancaire compte2 = new CompteBancaire();
        compte2.setId("54aa7db30bc460e1aeb95596");
        assertEquals("54aa7db30bc460e1aeb95596", (new ComptePanacheCodec().getDocumentId(compte2)).asString().getValue());
    }

}
