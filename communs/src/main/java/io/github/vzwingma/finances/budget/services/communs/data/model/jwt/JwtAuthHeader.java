package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Header d'un token JWT
 */
@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
public class JwtAuthHeader {
    private String alg;
    private String kid;
    private String typ;
}
