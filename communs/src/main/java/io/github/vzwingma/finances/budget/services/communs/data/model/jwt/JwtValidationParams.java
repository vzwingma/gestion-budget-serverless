package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
public class JwtValidationParams {

    /**
     * L'identifiant de l'application cliente.
     */
    private String idAppUserContent;

    /**
     * Les clés de signature JWT.
     */
    private List<JwksAuthKey> jwksAuthKeys;

    public void addJwksAuthKey(JwksAuthKey jwksAuthKey) {
        if (jwksAuthKeys == null) {
            jwksAuthKeys = new ArrayList<>();
        }
        jwksAuthKeys.add(jwksAuthKey);
    }
}
