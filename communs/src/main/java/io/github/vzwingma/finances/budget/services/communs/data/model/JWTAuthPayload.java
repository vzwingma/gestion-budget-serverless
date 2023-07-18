package io.github.vzwingma.finances.budget.services.communs.data.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RegisterForReflection
@Setter
@Getter
@NoArgsConstructor
public class JWTAuthPayload {
    private String iss;
    private String azp;
    private String aud;
    private String sub;
    private String email;
    private boolean email_verified;
    private String at_hash;
    private String name;
    private String picture;
    private String given_name;
    private String family_name;
    private String locale;
    private long iat;
    private long exp;

    @Override
    public String toString() {
        return "JWTPayload{" +
                "name='" + name + '\'' +
                ", iat=" + iat +
                ", exp=" + exp +
                '}';
    }
}
