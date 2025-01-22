package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurPrefsEnum;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.EnumMap;

/**
 * Mock data d'un utilisateur
 */
public class MockDataUtilisateur {


    public static Utilisateur getTestUtilisateur() {
        Utilisateur userOK = new Utilisateur();
        userOK.setId(new ObjectId("54aa7db30bc460e1aeb95596"));
        userOK.setLogin("Test");
        userOK.setDernierAcces(LocalDateTime.now());
        userOK.setPrefsUtilisateur(new EnumMap<>(UtilisateurPrefsEnum.class));
        userOK.getPrefsUtilisateur().put(UtilisateurPrefsEnum.PREFS_STATUT_NLLE_DEPENSE, "Nouvelle");
        return userOK;
    }

    public static Utilisateur getTestUtilisateurWithDate() {
        Utilisateur userOK = getTestUtilisateur();
        userOK.setDernierAcces(LocalDateTime.now());
        return userOK;
    }
}
