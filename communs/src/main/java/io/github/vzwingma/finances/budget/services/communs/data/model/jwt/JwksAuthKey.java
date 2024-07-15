package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * Header d'un token JWT
 */
@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
@ToString
public class JwksAuthKey {
    private String e;
    private String n;
    private String alg;
    private String kty;
    private String kid;
    private String use;
}
