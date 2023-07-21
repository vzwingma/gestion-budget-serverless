package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config.codec;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Enregistrement des codecs pour les échanges avec MongoDB
 */
public class RegisterPanacheCodecs implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Utilisateur.class)) {
            return (Codec<T>) new UtilisateurPanacheCodec();
        }
        return null;
    }

}
