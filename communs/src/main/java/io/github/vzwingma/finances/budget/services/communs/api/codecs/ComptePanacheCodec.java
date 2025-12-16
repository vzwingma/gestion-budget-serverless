package io.github.vzwingma.finances.budget.services.communs.api.codecs;


import com.mongodb.MongoClientSettings;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire.Proprietaire;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Panache Codec pour la classe {@link CompteBancaire}
 */
@ApplicationScoped
public class ComptePanacheCodec implements CollectibleCodec<CompteBancaire> {

    private static final Logger LOG = LoggerFactory.getLogger(ComptePanacheCodec.class);
    private final Codec<Document> documentCodec;

    @SuppressWarnings("null")
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
        // feat #67 & compatibilité asc : ajout des multiples propriétaires

        @SuppressWarnings("null")
        Document proprietaireDocument = document.get("proprietaire", Document.class);
        if(proprietaireDocument != null) {
            CompteBancaire.Proprietaire proprietaire = decode(proprietaireDocument);
            compteBancaire.setProprietaires(List.of(proprietaire));
        }
        @SuppressWarnings("null")
        List<Document> proprietairesDocument = document.getList("proprietaires", Document.class);
        if(proprietairesDocument != null) {
            compteBancaire.setProprietaires(new ArrayList<>());
            proprietairesDocument.forEach(prop -> {
                CompteBancaire.Proprietaire propTemp = decode(prop);
                compteBancaire.getProprietaires().add(propTemp);
            });
        }

        return compteBancaire;
    }

    @Override
    public void encode(BsonWriter bsonWriter, CompteBancaire compteBancaire, EncoderContext encoderContext) {
        LOG.warn("Encoding is not implemented");
    }

    @SuppressWarnings("null")
    @Override
    public Class<CompteBancaire> getEncoderClass() {
        return CompteBancaire.class;
    }


    /**
     * Décode un document MongoDB en un objet Proprietaire.
     *
     * @param document le document MongoDB à décoder
     * @return un objet Proprietaire avec les données du document
     */
    private Proprietaire decode(Document document) {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setId(document.getObjectId("_id"));
        proprietaire.setLogin(document.getString("login"));
        return proprietaire;
    }
}
