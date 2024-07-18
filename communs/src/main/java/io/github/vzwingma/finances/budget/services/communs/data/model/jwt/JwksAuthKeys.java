package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Liste des Clés de signature des tokens JWT
 */
@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
public class JwksAuthKeys {
    private JwksAuthKey[] keys;
}
