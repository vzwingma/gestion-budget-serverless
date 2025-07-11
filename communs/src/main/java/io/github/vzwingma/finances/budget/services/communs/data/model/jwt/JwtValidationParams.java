package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
public class JwtValidationParams {

    /**
     * L'identifiant de l'application cliente.
     */
    private String idAppUserContent;


}
