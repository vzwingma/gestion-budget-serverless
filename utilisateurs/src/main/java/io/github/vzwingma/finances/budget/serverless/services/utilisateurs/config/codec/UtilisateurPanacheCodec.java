package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config.codec;


import com.mongodb.MongoClientSettings;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurDroitsEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurPrefsEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

import java.util.Date;

/**
 * Panache Codec pour la classe Utilisateur
 */
@ApplicationScoped
public class UtilisateurPanacheCodec implements CollectibleCodec<Utilisateur> {

    private final Codec<Document> documentCodec;

    public UtilisateurPanacheCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public Utilisateur generateIdIfAbsentFromDocument(Utilisateur utilisateur) {
        utilisateur.setId(ObjectId.get());
        return utilisateur;
    }

    @Override
    public boolean documentHasId(Utilisateur utilisateur) {
        return utilisateur != null && utilisateur.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(Utilisateur utilisateur) {
        if (documentHasId(utilisateur)) {
            return new BsonString(utilisateur.getId().toString());
        } else {
            generateIdIfAbsentFromDocument(utilisateur);
            return getDocumentId(utilisateur);
        }
    }

    /**
     * Décodage de la classe {@link Utilisateur}
     *
     * @param bsonReader     reader du BSON issu de la BDD
     * @param decoderContext contexte
     * @return utilisateur lu
     */
    @Override
    public Utilisateur decode(BsonReader bsonReader, DecoderContext decoderContext) {
        Utilisateur utilisateur = new Utilisateur();
        Document document = documentCodec.decode(bsonReader, decoderContext);
        utilisateur.setId(document.getObjectId("_id"));
        utilisateur.setLogin(document.getString("login"));
        utilisateur.setDernierAcces(BudgetDateTimeUtils.getLocalDateTimeFromMillisecond(document.getDate("dernierAcces").getTime()));
        document.get("prefsUtilisateur", Document.class)
                .forEach((key, value) -> utilisateur.getPrefsUtilisateur().put(UtilisateurPrefsEnum.valueOf(key), value.toString()));

        document.get("droits", Document.class)
                .forEach((key, value) -> utilisateur.getDroits().put(UtilisateurDroitsEnum.valueOf(key), Boolean.valueOf(value.toString())));
        return utilisateur;
    }

    @Override
    public void encode(BsonWriter bsonWriter, Utilisateur utilisateur, EncoderContext encoderContext) {
        Document docUtilisateur = new Document();
        docUtilisateur.put("_id", utilisateur.getId());
        docUtilisateur.put("login", utilisateur.getLogin());
        docUtilisateur.put("dernierAcces", new Date(BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(utilisateur.getDernierAcces())));

        Document docPrefs = new Document();
        utilisateur.getPrefsUtilisateur().forEach((k, v) -> docPrefs.put(k.toString(), v));
        docUtilisateur.put("prefsUtilisateur", docPrefs);

        Document docDroits = new Document();
        utilisateur.getDroits().forEach((k, v) -> docDroits.put(k.toString(), v));
        docUtilisateur.put("droits", docDroits);

        documentCodec.encode(bsonWriter, docUtilisateur, encoderContext);
    }

    @Override
    public Class<Utilisateur> getEncoderClass() {
        return Utilisateur.class;
    }
}
