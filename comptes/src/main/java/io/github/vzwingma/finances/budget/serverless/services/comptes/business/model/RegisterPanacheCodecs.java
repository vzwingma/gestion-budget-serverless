package io.github.vzwingma.finances.budget.serverless.services.comptes.business.model;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.api.codecs.ComptePanacheCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Enregistrement des codecs pour les échanges avec MongoDB
 */
public class RegisterPanacheCodecs implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(CompteBancaire.class)) {
            return (Codec<T>) new ComptePanacheCodec();
        }
        return null;
    }

}
