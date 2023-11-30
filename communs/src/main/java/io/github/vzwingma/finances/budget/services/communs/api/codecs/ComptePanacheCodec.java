package io.github.vzwingma.finances.budget.services.communs.api.codecs;


import com.mongodb.MongoClientSettings;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Panache Codec pour la classe {@link CompteBancaire}
 */
@ApplicationScoped
public class ComptePanacheCodec implements CollectibleCodec<CompteBancaire> {

    private static final Logger LOG = LoggerFactory.getLogger(ComptePanacheCodec.class);
    private final Codec<Document> documentCodec;

    public ComptePanacheCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public CompteBancaire generateIdIfAbsentFromDocument(CompteBancaire compteBancaire) {
        compteBancaire.setId(UUID.randomUUID().toString());
        return compteBancaire;
    }

    @Override
    public boolean documentHasId(CompteBancaire compteBancaire) {
        return compteBancaire != null && compteBancaire.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(CompteBancaire compteBancaire) {
        if (documentHasId(compteBancaire)) {
            return new BsonString(compteBancaire.getId());
        } else {
            generateIdIfAbsentFromDocument(compteBancaire);
            return getDocumentId(compteBancaire);
        }
    }

    /**
     * Décodage de la classe {@link CompteBancaire}
     *
     * @param bsonReader     reader du BSON issu de la BDD
     * @param decoderContext contexte
     * @return utilisateur lu
     */
    @Override
    public CompteBancaire decode(BsonReader bsonReader, DecoderContext decoderContext) {
        CompteBancaire compteBancaire = new CompteBancaire();
        Document document = documentCodec.decode(bsonReader, decoderContext);
        compteBancaire.setId(document.getString("_id"));
        compteBancaire.setLibelle(document.getString("libelle"));
        compteBancaire.setActif(document.getBoolean("actif"));

        compteBancaire.setItemIcon(document.getString("itemIcon"));
        compteBancaire.setOrdre(document.getInteger("ordre"));

        CompteBancaire.Proprietaire proprietaire = new CompteBancaire.Proprietaire();
        Document proprietaireDocument = document.get("proprietaire", Document.class);
        proprietaire.setId(proprietaireDocument.getObjectId("_id"));
        proprietaire.setLibelle(proprietaireDocument.getString("libelle"));
        proprietaire.setLogin(proprietaireDocument.getString("login"));
        compteBancaire.setProprietaire(proprietaire);

        return compteBancaire;
    }

    @Override
    public void encode(BsonWriter bsonWriter, CompteBancaire compteBancaire, EncoderContext encoderContext) {
        LOG.warn("Encoding is not implemented");
    }

    @Override
    public Class<CompteBancaire> getEncoderClass() {
        return CompteBancaire.class;
    }
}
